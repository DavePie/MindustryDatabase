package net.ddns.mindustry.database.client.impl;

import net.ddns.mindustry.database.client.PunishmentListener;
import net.ddns.mindustry.database.client.PunishmentQueries;
import net.ddns.mindustry.database.schema.enums.PunishmentType;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jspecify.annotations.NullMarked;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

@NullMarked
public final class PunishmentListenerImpl implements PunishmentListener, AutoCloseable {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final ReentrantReadWriteLock locks = new ReentrantReadWriteLock();
    private final HashMap<Key, ArrayList<Consumer<Event>>> listeners = new HashMap<>();
    private final DatabaseImpl database;
    private final ScheduledFuture<?> notificationThread;

    public PunishmentListenerImpl(DatabaseImpl database) throws SQLException {
        this.database = Objects.requireNonNull(database);
        this.notificationThread = executor.scheduleAtFixedRate(this::checkUpdates, 2, 2, TimeUnit.SECONDS);
    }

    private void checkUpdates() {

        locks.readLock().lock();
        try {
            for (var entry : listeners.entrySet()) {
                final var key = entry.getKey();
                queryForResult(key.type(), key.server(), entry.getValue());
            }
        } finally {
            locks.readLock().unlock();
        }
    }

    private void queryForResult(PunishmentType type, Server server, ArrayList<Consumer<Event>> listeners) {

        final List<PunishmentQueries.Punishment> punishments;
        try { punishments = database.punishment().recentPunishments(type, server);
        } catch (Exception e) {
            final var failure = new Event.Failure(e);
            listeners.forEach(listener -> listener.accept(failure));
            return;
        }

        for (var punishment : punishments) {
            final var value = new Event.Value(punishment.id(), type, server);
            listeners.forEach(listener -> listener.accept(value));
        }
    }

    @Override
    public void close() {
        notificationThread.cancel(true);
        executor.shutdown();
    }

    @Override
    public void register(PunishmentType type, Server server, Consumer<Event> listener) {

        Objects.requireNonNull(type);
        Objects.requireNonNull(server);
        Objects.requireNonNull(listener);

        final var key = new Key(type, server);
        locks.writeLock().lock();
        try {
            listeners.computeIfAbsent(key, _ -> new ArrayList<>()).add(listener);
        } finally {
            locks.writeLock().unlock();
        }
    }

    @Override
    public void unregister(PunishmentType type, Server server, Consumer<Event> listener) {

        Objects.requireNonNull(type);
        Objects.requireNonNull(server);
        Objects.requireNonNull(listener);

        final var key = new Key(type, server);

        locks.writeLock().lock();
        try {

            final var list = listeners.get(key);
            if (list == null) return;

            list.remove(listener);
            if (list.isEmpty()) listeners.remove(key);

        } finally {
            locks.writeLock().unlock();
        }
    }

    private record Key(PunishmentType type, Server server) {}
}
