package net.ddns.mindustry.database.testclient.util;

import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.PunishmentListener;
import net.ddns.mindustry.database.schema.enums.PunishmentType;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.junit.jupiter.api.Assertions;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EventCycle {

    /// Registers an event listener with the provided type, executes the query, ensures the row id is sent via the listener, and unregisters the event listener.
    public static void verify(Database db, PunishmentType type, Server server, Supplier<Integer> query, Duration timeout) {

        Objects.requireNonNull(db);
        Objects.requireNonNull(type);
        Objects.requireNonNull(query);
        Objects.requireNonNull(timeout);

        final var queue = new LinkedBlockingQueue<PunishmentListener.Event>();
        final Consumer<PunishmentListener.Event> listener = queue::add;

        try {
            db.listeners().register(type, server, listener);
            Assertions.assertTimeoutPreemptively(timeout, () -> waitId(queue, query.get()));
        } finally {
            db.listeners().unregister(type, server, listener);
        }
    }

    private static void waitId(LinkedBlockingQueue<PunishmentListener.Event> queue, int rowId) {
        while (true) {

            final PunishmentListener.Event event;
            try { event = queue.take();
            } catch (InterruptedException e) {
                Assertions.fail("The test has been interrupted.", e);
                return; // Compiler needs the return here.
            }

            switch (event) {
                case PunishmentListener.Event.Value(int id, _, _) -> {
                    if (id == rowId) return;
                    // I don't fail since I'm running concurrently, meaning I might listen the id of a different task.
                }
                case PunishmentListener.Event.Failure(Exception e) -> Assertions.fail("Event failure returned.", e);
            }
        }
    }
}
