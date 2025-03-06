package net.ddns.mindustry.database.plugin.commands.clientCommands;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.game.Team;
import mindustry.gen.Player;
import net.ddns.mindustry.database.client.AccountQueries;
import net.ddns.mindustry.database.schema.tables.pojos.Account;

import java.util.Objects;
import java.util.Optional;

import static net.ddns.mindustry.database.plugin.Configs.configSessionDuration;
import static net.ddns.mindustry.database.plugin.Main.database;

public class PlayerCommands {
    public static void load(CommandHandler handler) {
        handler.register("login", "<username> <password>", "Logs you into your account. If you do" +
                " not have an account, then use the /signup command.", PlayerCommands::login);

        handler.register("signup", "<username> <display-name> <password> <password>", "Creates an" +
                        " account that you can log into with the [gold]/login[] command. You do need to type in the same" +
                        " password twice for the last two arguments. Keep in mind that the username you select is " +
                        " [blue]permanent[], meaning that you cannot change it once your account is made.",
                PlayerCommands::signup);

        handler.register("logout", "Logs you out of your current account and session.",
                PlayerCommands::logout);

        handler.register("change-display-name", "<new_display_name>", "Changes your display name.",
                PlayerCommands::changeDisplayName);

        handler.register("change-password", "<new_password> <new_password> <old_password>",
                "Changes your password to a new password.", PlayerCommands::changePassword);
    }

    private static void login(String[] args, Player player) {
        String username = args[0];
        String password = args[1];

        if (!configSessionDuration.isNum()) {
            Log.err("Session duration is not a number. Please ensure that you've entered a proper integer.");
            player.sendMessage("[scarlet]Command failed to run. Please contact an admin or staff member of this " +
                    "server.");

            return;
        }

        int duration = configSessionDuration.num();
        AccountQueries.LoginStatus loginResult = database.auth().login(username.strip(), password.toCharArray(),
                player.ip(), player.uuid(), duration);

        if (loginResult instanceof AccountQueries.LoginStatus.WrongCredentials) {
            player.sendMessage("[scarlet]The credentials provided were invalid. Please ensure that you've entered " +
                    "the correct credentials and that you've signed up.");
            return;
        } else if (loginResult instanceof AccountQueries.LoginStatus.AlreadyLoggedIn) {
            player.sendMessage("Already signed in.");
            return;
        } else if (loginResult instanceof AccountQueries.LoginStatus.LoggedIn) {
            String displayName = ((AccountQueries.LoginStatus.LoggedIn) loginResult).account().displayName();

            player.team(Team.sharded);
            player.name(displayName);
            player.sendMessage("Logged in successfully.");
            return;
        }

        player.sendMessage("An unknown status was received. Please contact a staff member of this server.");
        Log.warn("The login method returned a status that is not accounted for.");
        Log.warn(String.format("Returned status: %s", loginResult.getClass()));
    }

    private static void signup(String[] args, Player player) {
        String username = args[0];
        String displayName = args[1];
        String password = args[2];
        String passwordVerification = args[3];  // third argument provided by the player. Mostly to ensure that their
        // password are typed in correctly.

        if (!Objects.equals(password, passwordVerification)) {
            player.sendMessage("The passwords provided did not match. Please ensure that you've typed in your" +
                    " password correctly.");
            return;
        }

        AccountQueries.SignupStatus signupStatus = database.auth().signup(username.strip(), password.toCharArray(),
                displayName, player.ip(), player.uuid());

        if (signupStatus instanceof AccountQueries.SignupStatus.UsernameInUse) {
            player.sendMessage("[scarlet]The username that was provided is already in use.");
            return;

        } else if (signupStatus instanceof AccountQueries.SignupStatus.InvalidName || signupStatus instanceof
                AccountQueries.SignupStatus.InvalidPassword) {
            player.sendMessage("[scarlet]The username or password that was provided is invalid.");
            return;

        } else if (!(signupStatus instanceof AccountQueries.SignupStatus.Created)) {
            player.sendMessage("[scarlet]An unknown error has occurred. Please contact an admin or staff member of " +
                    "this server as soon as possible.");
            return;
        }

        player.sendMessage("Signup was successful. You can now log in with the /login command.");
    }

    private static void logout(String[] args, Player player) {
        Optional<Account> account = database.auth().find(player.ip(), player.uuid());

        if (account.isEmpty()) {
            player.sendMessage("[orange]There is either no active session or you're not logged in.");
            return;
        }

        database.auth().logout(account.get());
        player.team(Team.derelict);
        player.unit().kill();
        player.sendMessage("Logged out successfully.");
    }

    private static void changeDisplayName(String[] args, Player player) {
        String newDisplayName = args[0];
        Optional<Account> account = database.auth().find(player.ip(), player.uuid());

        if (account.isEmpty()) {
            player.sendMessage("[orange]There is either no active session or you're not logged in.");
            return;
        }

        database.auth().updateDisplayName(account.get(), newDisplayName);
        player.name(newDisplayName);
        player.sendMessage("Display name was successfully updated.");
    }

    private static void changePassword(String[] args, Player player) {
        String newPassword = args[0];
        String passwordVerification = args[1];
        String oldPassword = args[2];
        Optional<Account> account = database.auth().find(player.ip(), player.uuid());

        if (account.isEmpty()) {
            player.sendMessage("[orange]There is either no active session or you're not logged in.");
            return;
        }

        if (!newPassword.equals(passwordVerification)) {
            player.sendMessage("[scarlet]Passwords do not match!");
            return;
        }

        AccountQueries.PasswordUpdateStatus status = database.auth().updatePassword(account.get(),
                newPassword.toCharArray(), oldPassword.toCharArray());

        if (status instanceof AccountQueries.PasswordUpdateStatus.InvalidPassword) {
            player.sendMessage("[scarlet]The old password that you provided was invalid.");
            return;
        }

        // I typically provide an "uh-oh" case, but I'm not too hell-bent on writing it here. + I don't feel like
        // making a method specifically so that I'm not repeating myself constantly.
        player.sendMessage("Password was changed successfully.");
    }
}
