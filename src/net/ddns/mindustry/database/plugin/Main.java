package net.ddns.mindustry.database.plugin;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.mod.Plugin;
import net.ddns.mindustry.database.client.Database;
import net.ddns.mindustry.database.plugin.Commands.ClientCommands;
import net.ddns.mindustry.database.plugin.Commands.ServerCommands;

public class Main extends Plugin {
    public static Database database;

    public void init() {
        Configs.load();
        Events.load();

        database = Utilities.newDatabase();

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
