package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.*;
import org.jspecify.annotations.NullMarked;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NullMarked
public interface PunishmentQueries {

    Optional<Issuer> findIssuer(int id);

    List<Ban> activeBans(Account account);

    Optional<Ban> findBan(long uuid);

    Optional<Ban> findBan(int id);

    Optional<Kick> findKick(int id);

    Optional<Warn> findWarn(int id);

    Ban infiteBan(Account punished, Issuer issuer, String reason, Server server);

    Ban ban(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime expiration);

    Ban ban(Account punished, Issuer issuer, String reason, Server server, Duration duration);

    Kick kick(Account punished, Issuer issuer, String reason, Server server);

    Warn warn(Account punished, Issuer issuer, String reason, Server server);

    UnbanStatus unban(Ban ban, Issuer issuer);

    /// @param unban the row of the issued unban.
    /// @param alreadyUnbanned true if the ban has already been unbanned, false if the ban has just been unbanned.
    record UnbanStatus(Unban unban, boolean alreadyUnbanned) {
        public UnbanStatus {
            Objects.requireNonNull(unban);
        }
    }

    @NullMarked
    sealed interface Issuer {

        static Issuer of(Server server) {
            return new Console(server);
        }

        static Issuer of(Account account) {
            return new Player(account);
        }

        record Console(Server server) implements Issuer {
            public Console {
                Objects.requireNonNull(server);
            }
        }

        record Player(Account account) implements Issuer {
            public Player {
                Objects.requireNonNull(account);
            }
        }
    }
}
