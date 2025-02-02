package net.ddns.mindustry.moderation_system;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.mod.Plugin;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.SecurityConfig;
import net.ddns.mindustry.moderation_system.Client.ClientCommands;
import net.ddns.mindustry.moderation_system.Server.ServerCommands;
import net.ddns.mindustry.moderation_system.Server.ConfigConstants;

import java.security.NoSuchAlgorithmException;

import static net.ddns.mindustry.moderation_system.Server.ConfigConstants.*;

public class Main extends Plugin {
    Database database;

    public void init() {
        ConfigConstants.load();
        SecurityConfig securityConfig;

        try {
            securityConfig = new SecurityConfig("SHA-256", 32, 255, 10, 69_000, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            database = Database.newConnection("jdbc:mariadb://" + configURL.string() + "/mindustry_database",
                    configUser.string(), configPassword.string(), securityConfig);
        } catch (Exception e) {
            Log.err(e);
            Log.info("Moderation plugin will exit.");
            return;
        }

        Log.debug("Connection successful!");
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        ServerCommands.load(handler);
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        ClientCommands.load(handler);
    }
}
