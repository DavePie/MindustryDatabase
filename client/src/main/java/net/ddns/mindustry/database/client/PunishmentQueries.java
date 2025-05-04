package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PunishmentQueries {

    // TODO Ask username instead of account
    List<Ban> activeBans(Account account);

    Optional<Ban> findBan(long uuid);

    Optional<Ban> findBan(int id);

    Optional<Kick> findKick(int id);

    Optional<Warn> findWarn(int id);

    Status<Ban> ban(String punishedUsername, String staffUsername, String reason, Server server, OffsetDateTime expiration);

    Status<Kick> kick(String punishedUsername, String staffUsername, String reason, Server server);

    Status<Warn> warn(String punishedUsername, String staffUsername, String reason, Server server);

    UnbanStatus unban(Ban ban, String staffUsername);

    sealed interface Status<T> {
        record PunishedNotFound<T>() implements Status<T> {}
        record StaffNotFound<T>() implements Status<T> {}
        record Ok<T>(T punishment) implements Status<T> {}
    }

    enum UnbanStatus {
        STAFF_NOT_FOUND,
        ALREADY_UNBANNED,
        OK
    }
}
