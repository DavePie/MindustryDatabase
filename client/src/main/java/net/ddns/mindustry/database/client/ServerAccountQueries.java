package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import java.util.Objects;

public interface ServerAccountQueries {

    JoinStatus joinsServer(Server server, String displayName, String ip, String uuid);

    /// @return false if the account is offline, otherwise true.
    boolean leavesServer(Account account, Server server);

    boolean isAccountWhitelisted(Account account, Server server);

    void whitelistAccount(Account account, Server server);

    void removeAccountWhitelist(Account account, Server server);

    sealed interface JoinStatus {

        /// The account is authenticated and joined in the server.
        record Joined(Account account) implements JoinStatus {
            public Joined {
                Objects.requireNonNull(account);
            }
        }

        /// The account is already connected in this or another server.
        record AlreadyInServer() implements JoinStatus {}

        /// The account is not authenticated.
        record NotAuthenticated() implements JoinStatus {}

        /// The account does not have enough authorizations to join this server.
        record NotWhitelisted() implements JoinStatus {}
    }
}
