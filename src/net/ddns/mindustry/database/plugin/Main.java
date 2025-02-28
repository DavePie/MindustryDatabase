package net.ddns.mindustry.database.plugin;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.mod.Plugin;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.plugin.Commands.ClientCommands;
import net.ddns.mindustry.database.plugin.Commands.ServerCommands;

import java.util.logging.LogManager;

public class Main extends Plugin {
    public static Database database;

    // I'm going to kill myself is jOOQ sends another self-ad
    static {
        LogManager.getLogManager().reset();
    }

    public void init() {
        // https://stackoverflow.com/a/5762502
        Log.info("\u001B[34mPowered by jOOQ.\u001B[0m");

        Configs.load();
        Events.load();
        net.ddns.mindustry.database.plugin.Commands.CommandHandler.load(); // oops...

        database = Utilities.newDatabase();

        PeriodicTasks.load();

        Log.debug("Database connection successful!");
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
