package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public interface PunishmentQueries {

    // TODO Ask username instead of account? Or have one for username and one for id?
    List<Ban> activeBans(Account account);

    Optional<Ban> findBan(long uuid);

    Optional<Ban> findBan(int id);

    Optional<Kick> findKick(int id);

    Optional<Warn> findWarn(int id);

    Status<Ban> ban(String punishedUsername, String staffUsername, String reason, Server server, @Nullable OffsetDateTime expiration);

    Status<Kick> kick(String punishedUsername, String staffUsername, String reason, Server server);

    Status<Warn> warn(String punishedUsername, String staffUsername, String reason, Server server);

    UnbanStatus unban(Ban ban, String staffUsername);

    sealed interface Status<T> {

        record PunishedNotFound<T>() implements Status<T> {}

        record StaffNotFound<T>() implements Status<T> {}

        record Ok<T>(T punishment) implements Status<T> {
            public Ok {
                Objects.requireNonNull(punishment);
            }
        }
    }

    enum UnbanStatus {
        STAFF_NOT_FOUND,
        ALREADY_UNBANNED,
        OK
    }
}
