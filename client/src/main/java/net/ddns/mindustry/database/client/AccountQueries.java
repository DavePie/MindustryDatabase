package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import net.ddns.mindustry.database.schema.tables.pojos.Server;
import org.jooq.exception.DataAccessException;
import java.util.Optional;

public interface AccountQueries {

    boolean isUsernameValid(String username);

    /// Searches the account with this username.
    /// @return the account if found.
    Optional<Account> find(String username) throws DataAccessException;

    /// Searches for an account using their IP and UUID. Requires the player to have a session.
    /// @return the account if found.
    Optional<Account> find(String ip, String uuid) throws DataAccessException;

    /// Does a login attempt and if successful, creates a new session with the provided duration.
    /// @param username the username of the account.
    /// @param password the password of the account.
    /// @param ip the ip of player used
    /// @param durationHours the duration of the session in hours.
    LoginStatus login(String username, char[] password, String ip, String uuid, int durationHours) throws DataAccessException;

    void logout(Account account) throws DataAccessException;

    /// Creates a new account with this username and password.
    /// @param username the new account username.
    /// @param password the new account password.
    /// @param ip the player address for internal checks.
    /// @param uuid the player uuid for internal checks.
    SignupStatus signup(String username, char[] password, String displayName, String ip, String uuid);

    JoinStatus joinsServer(Server server, String ip, String uuid) throws DataAccessException;

    void leavesServer(Account account) throws DataAccessException;

    /// Updates the display name of an account.
    void updateDisplayName(Account account, String newDisplayName) throws DataAccessException;

    /// Updates the password of an account.
    /// @param newPassword The new password.
    /// @param oldPassword The old password.
    PasswordUpdateStatus updatePassword(Account account, char[] newPassword, char[] oldPassword) throws DataAccessException;

    sealed interface LoginStatus {

        /// The credentials are correct and the account has logged in.
        record LoggedIn(Account account) implements LoginStatus {}

        /// The username or password provided are wrong.
        record WrongCredentials() implements LoginStatus {}

        /// The account is already logged in and does not require authentication.
        record AlreadyLoggedIn() implements LoginStatus {}
    }

    sealed interface SignupStatus {

        record Created(Account account) implements SignupStatus {}

        record InvalidName() implements SignupStatus {}

        record InvalidPassword() implements SignupStatus {}

        /// The username that was provided by the user is already in use.
        record UsernameInUse() implements SignupStatus {}
    }

    sealed interface JoinStatus {

        /// The account is authenticated and joined in the server.
        record Joined(Account account) implements JoinStatus {}

        /// The account is already connected in this or another server.
        record AlreadyInServer() implements JoinStatus {}

        /// The account session has expired, and the account must re-authenticate.
        /// @deprecated to simplify the authentication logic, this has been moved inside {@link NotAuthenticated}.
        @Deprecated(forRemoval = true)
        record SessionExpired() implements JoinStatus {}

        /// The account is not authenticated.
        record NotAuthenticated() implements JoinStatus {}

        /// The account does not have enough authorizations to join this server.
        record NotAuthorized() implements JoinStatus {}
    }

    sealed interface PasswordUpdateStatus {
        record Updated() implements PasswordUpdateStatus {}

        record InvalidPassword() implements PasswordUpdateStatus {}
    }
}
