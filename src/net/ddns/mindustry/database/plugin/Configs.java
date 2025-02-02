package net.ddns.mindustry.database.plugin;

import mindustry.net.Administration;

public class Configs {
    protected static Administration.Config configURL;
    protected static Administration.Config configUser;
    protected static Administration.Config configPassword;

    public static Administration.Config configServerIP;
    public static Administration.Config configSessionDuration;

    protected static void load() {
        configURL = new Administration.Config("url", "The URL for the database.", "");
        configUser = new Administration.Config("user", "The user for the database.", "");
        configPassword = new Administration.Config("password", "The password for the database user.", "");

        configServerIP = new Administration.Config("server-ip", "The IP of the server.", "127.0.0.1");
        configSessionDuration = new Administration.Config("session-duration", "The maximum duration of a session.",
                12);
    }
}