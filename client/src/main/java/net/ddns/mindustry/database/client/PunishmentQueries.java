package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Ban;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PunishmentQueries {

    Optional<Ban> findBan(long uuid);

    Status ban(String punishedUsername, String staffUsername, String reason, Server server, LocalDateTime expiration);

    default void permBan(String username, String staff, Server server, String reason) {
        ban(username, staff, reason, server, null);
    }

    Status kick(String punishedUsername, String staffUsername, String reason, Server server);

    Status warn(String punishedUsername, String staffUsername, String reason, Server server);

    UnbanStatus unban(Ban ban, String staffUsername);

    enum Status {
        USERNAME_NOT_FOUND,
        STAFF_NOT_FOUND,
        OK
    }

    enum UnbanStatus {
        STAFF_NOT_FOUND,
        ALREADY_UNBANNED,
        OK
    }
}
