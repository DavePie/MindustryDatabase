package net.ddns.mindustry.moderation_system.Server;

import mindustry.net.Administration;

public class ConfigConstants {
    public static Administration.Config configURL;
    public static Administration.Config configUser;
    public static Administration.Config configPassword;

    public static void load() {
        configURL = new Administration.Config("url", "The URL for the database.", "");
        configUser = new Administration.Config("user", "The user for the database.", "");
        configPassword = new Administration.Config("password", "The password for the database user.", "");
    }
}
