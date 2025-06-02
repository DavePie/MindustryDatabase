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
import java.util.function.IntConsumer;

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
        this.pgCon = Objects.requireNonNull(database.dsl().configuration()
                        .connectionProvider()
                        .acquire(), "Could not retrieve the DSLContext connection.")
                .unwrap(PgConnection.class);

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
        if (Thread.interrupted()) return; // The close() method has been called.
        try {
            // Unfortunately, I'm forced to do this; else the notifications will not get updated.
            database.dsl().selectOne().execute();
            final PGNotification[] notifications = pgCon.getNotifications();
            if (notifications == null) return; // No notifications to listen to.

            for (var notification : notifications) {
                final Type channel = typeFromChannel(notification.getName());
                final int id = Integer.parseInt(notification.getParameter());
                trigger(channel, id);
            }
        } catch (NumberFormatException | SQLException e) {
            System.out.println("exception: " + e);
            // TODO Cannot handle it in any way unless with a log.
        }
    }

    @Override
    public void close() {
        notificationThread.cancel(true);
        executor.shutdown();
    }

    @Override
    public void register(Type type, IntConsumer listener) {

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
    public void unregister(Type type, IntConsumer listener) {

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

    private void trigger(Type type, int value) {

        Objects.requireNonNull(type);
        final var holder = listeners.get(type);

        // A copy to avoid the concurrent modification exception.
        final List<IntConsumer> copy;
        holder.locks().readLock().lock();
        try {
            copy = Collections.unmodifiableList(holder.listeners());
        } finally {
            holder.locks().readLock().unlock();
        }
        for (var listener : copy) {
            Thread.ofVirtual().start(() -> listener.accept(value));
        }
    }

    private record Holder(ArrayList<IntConsumer> listeners, ReentrantReadWriteLock locks) {
        private Holder() {
            this(new ArrayList<>(), new ReentrantReadWriteLock());
        }
    }
}
