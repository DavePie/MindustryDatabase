package net.ddns.mindustry.database.plugin.commands.clientCommands;

import arc.util.CommandHandler;
import net.ddns.mindustry.database.plugin.commands.clientCommands.moderation.PrivilegedCommands;

public class ClientCommands {
    public static void load(CommandHandler handler) {
        UnprivilegedCommands.load(handler);
        net.ddns.mindustry.database.plugin.commands.clientCommands.moderation.UnprivilegedCommands.load(handler);
        PrivilegedCommands.load(handler);
    }
}
