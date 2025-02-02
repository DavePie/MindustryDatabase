package net.ddns.mindustry.database.plugin.Commands;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.gen.Player;

import static net.ddns.mindustry.database.plugin.Configs.configSessionDuration;
import static net.ddns.mindustry.database.plugin.Main.database;

public class ClientCommands {
    public static void load(CommandHandler handler) {
        handler.register("login", "<username> <password>", ClientCommands::login);
    }

    public static void login(String[] args, Player player) {
        String username = args[0];
        String password = args[1];

        if (!configSessionDuration.isNum()) {
            Log.err("Session duration is not a number. Please ensure that you've entered a proper integer.");
            player.sendMessage("[scarlet]Command failed to run. Please contact an admin or staff member of this " +
                    "server.");

            return;
        }

        int duration = configSessionDuration.num();
        database.auth().login(username, password.toCharArray(), player.ip(), player.uuid(), duration);
        player.sendMessage("Logged in successfully.");
    }
}
