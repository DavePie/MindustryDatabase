package net.ddns.mindustry.database.javaPlugin;

import mindustry.game.Team;
import mindustry.gen.Player;

import static mindustry.Vars.netServer;

public class ChatFilters {
    protected static void load() {
        netServer.admins.chatFilters.add(ChatFilters::noDerelict);
    }

    // Remember, no derelict.
    private static String noDerelict(Player player, String message) {
        return (player.team().id == Team.derelict.id) && (!message.startsWith("/")) ? null : message;
    }
}
