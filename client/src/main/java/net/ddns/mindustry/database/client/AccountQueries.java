package net.ddns.mindustry.database.client;

import net.ddns.mindustry.database.schema.tables.pojos.Account;
import org.jspecify.annotations.Nullable;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public interface AccountQueries {

    boolean isUsernameValid(@Nullable String username);

    /// Searches the account with this username.
    /// @return the account if found.
    Optional<Account> find(String username);

    /// Searches for an account using their IP and UUID. Requires the player to have a session.
    /// @return the account if found.
    Optional<Account> find(String ip, String uuid);

    /// Gets an account via its ID.
    /// @return the account if found.
    Optional<Account> find(int id);

    Set<Account> findAccounts(String ip);

    /// Does a login attempt and if successful, creates a new session with the provided duration.
    /// @param username the username of the account.
    /// @param password the password of the account.
    /// @param ip the ip of player used
    /// @param sessionDuration the duration of the session.
    /// @apiNote the password will be wiped after calling this method.
    LoginStatus login(String username, char[] password, String ip, String uuid, Duration sessionDuration);

    void logout(Account account);

    /// Creates a new account with this username and password.
    /// @param username the new account username.
    /// @param password the new account password.
    /// @param ip the player address for internal checks.
    /// @param uuid the player uuid for internal checks.
    /// @param sessionDuration the duration of the session.
    /// @apiNote the password will be wiped after calling this method.
    SignupStatus signup(String username, char[] password, String ip, String uuid, Duration sessionDuration);

    /// Updates the password of an account.
    /// @param newPassword The new password.
    /// @param oldPassword The old password.
    /// @apiNote The oldPassword and newPassword will be wiped after calling this method.
    PasswordUpdateStatus updatePassword(Account account, char[] oldPassword, char[] newPassword);

    sealed interface LoginStatus {

        /// The credentials are correct and the account has logged in.
        record LoggedIn(Account account) implements LoginStatus {
            public LoggedIn {
                Objects.requireNonNull(account);
            }
        }

        /// The username or password provided are wrong.
        record WrongCredentials() implements LoginStatus {}

        /// The account is already logged in and does not require authentication.
        record AlreadyLoggedIn() implements LoginStatus {}
    }

    sealed interface SignupStatus {

        record Created(Account account) implements SignupStatus {
            public Created {
                Objects.requireNonNull(account);
            }
        }

        record InvalidUsername(String username) implements SignupStatus {
            public InvalidUsername {
                Objects.requireNonNull(username);
            }
        }

        ///  When the password does not fit security criteria.
        record InvalidPassword() implements SignupStatus {}

        /// The username provided by the user is already in use.
        record UsernameInUse(String username) implements SignupStatus {
            public UsernameInUse {
                Objects.requireNonNull(username);
            }
        }

        /// The user reached its limit of account creations.
        record LimitReached(int limit) implements SignupStatus {}
    }

    sealed interface PasswordUpdateStatus {

        /// When the old password is not valid.
        record WrongPassword() implements PasswordUpdateStatus {}

        ///  When the new password does not fit security criteria.
        record InvalidPassword() implements PasswordUpdateStatus {}

        record Updated() implements PasswordUpdateStatus {}
    }
}
