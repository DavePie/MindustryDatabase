package net.ddns.mindustry.database.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ddns.mindustry.database.client.PunishmentListeners;
import net.ddns.mindustry.database.schema.Tables;
import net.ddns.mindustry.database.schema.tables.pojos.Ban;
import net.ddns.mindustry.database.schema.tables.pojos.Kick;
import net.ddns.mindustry.database.schema.tables.pojos.Warn;
import org.jooq.DSLContext;
import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PunishmentListenersImpl implements PunishmentListeners, AutoCloseable {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final ObjectMapper mapper = new ObjectMapper();
    private final ArrayList<Consumer<Ban>>  banListeners  = new ArrayList<>();
    private final ArrayList<Consumer<Kick>> kickListeners = new ArrayList<>();
    private final ArrayList<Consumer<Warn>> warnListeners = new ArrayList<>();
    private final DSLContext dsl;
    private final PgConnection pgCon;
    private final ScheduledFuture<?> notificationThread;

    public PunishmentListenersImpl(DSLContext dsl) throws SQLException {
        this.dsl = Objects.requireNonNull(dsl);
        final var con = dsl.configuration()
                .connectionProvider()
                .acquire();
        Objects.requireNonNull(con, "Could not retrieve the DSLContext connection.");
        this.pgCon = con.unwrap(PgConnection.class);
        this.notificationThread = executor.scheduleAtFixedRate(this::notificationListener, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void registerBanListener(Consumer<Ban> listener) {
        Objects.requireNonNull(listener);
        synchronized (banListeners) {
            banListeners.add(listener);
        }
    }

    @Override
    public void unregisterBanListener(Consumer<Ban> listener) {
        Objects.requireNonNull(listener);
        synchronized (banListeners) {
            banListeners.remove(listener);
        }
    }

    @Override
    public void registerKickListener(Consumer<Kick> listener) {
        Objects.requireNonNull(listener);
        synchronized (kickListeners) {
            kickListeners.add(listener);
        }
    }

    @Override
    public void unregisterKickListener(Consumer<Kick> listener) {
        Objects.requireNonNull(listener);
        synchronized (kickListeners) {
            kickListeners.remove(listener);
        }
    }

    @Override
    public void registerWarnListener(Consumer<Warn> listener) {
        Objects.requireNonNull(listener);
        synchronized (warnListeners) {
            warnListeners.add(listener);
        }
    }

    @Override
    public void unregisterWarnListener(Consumer<Warn> listener) {
        Objects.requireNonNull(listener);
        synchronized (warnListeners) {
            warnListeners.remove(listener);
        }
    }

    private void triggerBan(Ban ban) {
        Objects.requireNonNull(ban);
        synchronized (banListeners) {
            for (var listener : banListeners) {
                Thread.ofVirtual().start(() -> listener.accept(ban));
            }
        }
    }

    private void triggerKick(Kick kick) {
        Objects.requireNonNull(kick);
        synchronized (kickListeners) {
            for (var listener : kickListeners) {
                Thread.ofVirtual().start(() -> listener.accept(kick));
            }
        }
    }

    private void triggerWarn(Warn warn) {
        Objects.requireNonNull(warn);
        synchronized (warnListeners) {
            for (var listener : warnListeners) {
                Thread.ofVirtual().start(() -> listener.accept(warn));
            }
        }
    }

    private void notificationListener() {
        if (Thread.interrupted()) return; // The close() method has been called.
        try {
            // Unfortunately, I'm forced to do this; else the notifications will not get updated.
            dsl.selectOne().execute();
            final PGNotification[] notifications = pgCon.getNotifications();
            if (notifications == null) return; // No notifications to listen to.

            final String prefix = "channel_insert_";
            for (var notification : notifications) {

                final String name = notification.getName();
                final String data = notification.getParameter();

                if      (name.equals(prefix + Tables.BAN.getName ())) triggerBan (mapper.readValue(data, Ban.class));
                else if (name.equals(prefix + Tables.KICK.getName())) triggerKick(mapper.readValue(data, Kick.class));
                else if (name.equals(prefix + Tables.WARN.getName())) triggerWarn(mapper.readValue(data, Warn.class));
            }
        } catch (JsonProcessingException | SQLException ignored) {
            // TODO Cannot handle it in any way unless with a log.
        }
    }

    @Override
    public void close() {
        notificationThread.cancel(true);
        executor.shutdown();
    }
}
