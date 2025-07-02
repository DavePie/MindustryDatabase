package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.DatabaseEvents;
import net.ddns.mindustry.database.schema.Tables;
import org.jspecify.annotations.NullMarked;
import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

@NullMarked
public final class DatabaseEventsImpl implements DatabaseEvents, AutoCloseable {

    private static final String PREFIX = "channel_insert_";
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final HashMap<Type, Holder> listeners = new HashMap<>();
    private final DatabaseImpl database;
    private final PgConnection pgCon;
    private final ScheduledFuture<?> notificationThread;

    public DatabaseEventsImpl(DatabaseImpl database) throws SQLException {

        this.database = Objects.requireNonNull(database);
        this.pgCon = database.connection().unwrap(PgConnection.class);

        // I initialize the holders and I start to listen.
        for (Type type : Type.values()) {
            listeners.put(type, new Holder());
            database.dsl().execute(listenSqlFor(type));
        }
        this.notificationThread = executor.scheduleAtFixedRate(this::notificationListener, 1, 1, TimeUnit.SECONDS);
    }

    private static String listenSqlFor(Type type) {
        final String name = (switch (type) {
            case BAN  -> Tables.BAN;
            case KICK -> Tables.KICK;
            case WARN -> Tables.WARN;
            case MUTE -> Tables.MUTE;
        }).getName();
        return "LISTEN " + PREFIX + name;
    }

    private static Type typeFromChannel(String channel) {
        if (channel.equals(PREFIX + Tables.BAN .getName())) return Type.BAN;
        if (channel.equals(PREFIX + Tables.KICK.getName())) return Type.KICK;
        if (channel.equals(PREFIX + Tables.WARN.getName())) return Type.WARN;
        if (channel.equals(PREFIX + Tables.MUTE.getName())) return Type.MUTE;
        throw new IllegalStateException("Could not handle channel: " + channel);
    }

    private void notificationListener() {
        if (Thread.currentThread().isInterrupted()) return; // The close() method has been called.
        try {
            // Unfortunately, I'm forced to do this; else the notifications will not get updated.
            database.dsl().selectOne().execute();
            final PGNotification[] notifications = pgCon.getNotifications();
            if (notifications == null) return; // No notifications to listen to.

            for (var notification : notifications) {
                final Type channel = typeFromChannel(notification.getName());
                final int id = Integer.parseInt(notification.getParameter());
                onEvent(channel, id);
            }
        } catch (NumberFormatException | SQLException e) {
            onFailure(e);
        }
    }

    @Override
    public void close() {
        notificationThread.cancel(true);
        executor.shutdown();
        // I unlisten on all channels.
        database.dsl().execute("UNLISTEN *;");
    }

    @Override
    public void register(Type type, Consumer<Event> listener) {

        Objects.requireNonNull(type);
        Objects.requireNonNull(listener);
        final var holder = listeners.get(type);

        holder.locks().writeLock().lock();
        try {
            holder.listeners().add(listener);
        } finally {
            holder.locks().writeLock().unlock();
        }
    }

    @Override
    public void unregister(Type type, Consumer<Event> listener) {

        Objects.requireNonNull(type);
        Objects.requireNonNull(listener);
        final var holder = listeners.get(type);

        holder.locks().writeLock().lock();
        try {
            holder.listeners().remove(listener);
        } finally {
            holder.locks().writeLock().unlock();
        }
    }

    private void trigger(Holder holder, Event event) {

        Objects.requireNonNull(holder);
        Objects.requireNonNull(event);

        // A copy to avoid the concurrent modification exception.
        holder.locks().readLock().lock();
        try {
            for (var listener : holder.listeners) {
                Thread.ofVirtual().start(() -> listener.accept(event));
            }
        } finally {
            holder.locks().readLock().unlock();
        }
    }

    private void onEvent(Type type, int id) {
        Objects.requireNonNull(type);
        final var holder = listeners.get(type);
        // I avoid blocking the Thread that listen for notifications.
        Thread.ofVirtual().start(() -> trigger(holder, new Event.Value(id)));
    }

    private void onFailure(Exception exception) {
        Objects.requireNonNull(exception);
        final Collection<Holder> holders = listeners.values();
        // I avoid blocking the Thread that listen for notifications.
        Thread.ofVirtual().start(() -> {
            for (var holder : holders) {
                trigger(holder, new Event.Failure(exception));
            }
        });
    }

    private record Holder(ArrayList<Consumer<Event>> listeners, ReentrantReadWriteLock locks) {
        private Holder() {
            this(new ArrayList<>(), new ReentrantReadWriteLock());
        }
    }
}
