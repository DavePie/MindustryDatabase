package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface PunishmentQueries {

    Optional<Issuer> findIssuer(int id);

    Optional<Ban> latestBan();

    Optional<Mute> latestMute();

    Optional<Warn> latestUnseenWarn();

    List<Warn> unseenWarns();

    void markWarnSeen(Warn warn);

    List<Ban> activeBans(Account account);

    String uidFrom(Ban ban);

    Optional<Ban> findBan(String uid);

    Optional<Ban> findBan(int id);

    Optional<Ban> findBan(Account account);

    Optional<Kick> findKick(int id);

    Optional<Warn> findWarn(int id);

    Optional<Mute> findMute(int id);

    Ban infiteBan(Account punished, Issuer issuer, String reason, Server server);

    Ban ban(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime creation, OffsetDateTime expiration);

    Ban ban(Account punished, Issuer issuer, String reason, Server server, Duration duration);

    Kick kick(Account punished, Issuer issuer, String reason, Server server);

    Warn warn(Account punished, Issuer issuer, String reason, Server server);

    UnbanStatus unban(Ban ban, Issuer issuer);

    Mute mute(Account punished, Issuer issuer, String reason, Server server, OffsetDateTime creation, OffsetDateTime expiration);

    Mute mute(Account punished, Issuer issuer, String reason, Server server, Duration duration);

    /// @param unban the row of the issued unban.
    /// @param alreadyUnbanned true if the ban has already been unbanned, false if the ban has just been unbanned.
    record UnbanStatus(Unban unban, boolean alreadyUnbanned) {
        public UnbanStatus {
            Objects.requireNonNull(unban);
        }
    }

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
