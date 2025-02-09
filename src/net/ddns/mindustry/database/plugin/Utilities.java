package net.ddns.mindustry.database.plugin;

import arc.util.Log;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.client.SecurityConfig;

import java.security.NoSuchAlgorithmException;

import static net.ddns.mindustry.database.plugin.Configs.*;

public class Utilities {
    /**
     * Makes and returns a new `Database` object.
     * @return `Database`
     */
    public static Database newDatabase() {
        Database database;
        SecurityConfig securityConfig;

        try {
            securityConfig = new SecurityConfig("SHA-256", 32, 255, 10,
                    69_000, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            database = Database.newConnection("jdbc:postgresql://" + configURL.string() + "/mindustry_database",
                    configUser.string(), configPassword.string(), securityConfig);
        } catch (Exception e) {
            Log.debug(e);
            Log.warn("Ensure that the URL, the user, and the user's password is correct.");
            return null;
        }

        return database;
    }
}
