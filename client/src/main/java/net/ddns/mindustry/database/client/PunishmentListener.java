package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.enums.PunishmentType;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import java.util.Objects;
import java.util.function.Consumer;

/// Uses {@link PunishmentQueries#recentPunishments(PunishmentType, Server)} to query for events.
public interface PunishmentListener {

    void register(PunishmentType type, Server server, Consumer<Event> listener);

    void unregister(PunishmentType type, Server server, Consumer<Event> listener);

    sealed interface Event {

        record Value(int id, PunishmentType type, Server server) implements Event {}

        record Failure(Exception exception) implements Event {
            public Failure {
                Objects.requireNonNull(exception);
            }
        }
    }
}
