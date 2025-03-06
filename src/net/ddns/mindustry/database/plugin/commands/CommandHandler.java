package net.ddns.mindustry.database.plugin.commands;

import mindustry.gen.Player;

import static mindustry.Vars.netServer;

/**
 * Custom command handler for handling commands. This is to help ensure that a player doesn't have their password
 * logged.
 */
public class CommandHandler {
    private static final String secretPrefix = "\n\n By the nine I'm tweaking!! \n\n";

    public static void load() {
        netServer.clientCommands.setPrefix(secretPrefix);
        netServer.admins.chatFilters.add(CommandHandler::handler);
    }

    private static String handler(Player player, String message) {
        netServer.clientCommands.setPrefix("/");
        arc.util.CommandHandler.CommandResponse commandResponse = netServer.clientCommands.handleMessage(message, player);
        netServer.clientCommands.setPrefix(secretPrefix);

        // Remember kids, do NOT forget about your Log.info(message); lines. You might get confused. I learnt that the
        // hard way.

        if (commandResponse.type == arc.util.CommandHandler.ResponseType.noCommand) {
            return message;
        }

        if (commandResponse.type != arc.util.CommandHandler.ResponseType.valid) {
            String result = HandleInvalid(commandResponse);

            player.sendMessage(result);
        }

        return null;
    }

    /**
     * Handles any invalid commands. Essentially just a slightly stripped down version of
     * <a href="https://github.com/Anuken/Mindustry/blob/3222a5bd5067b66ccfb2392f191406d1b6ba9df4/core/src/mindustry/core/NetServer.java#L73-L96">
     *     this</a>
     * @param response The response of the command
     * @return A string representing what should be sent to the player.
     */
    private static String HandleInvalid(arc.util.CommandHandler.CommandResponse response) {
        if(response.type == arc.util.CommandHandler.ResponseType.manyArguments){
            return "[scarlet]Too many arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
        }else if(response.type == arc.util.CommandHandler.ResponseType.fewArguments){
            return "[scarlet]Too few arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
        }

        return "[scarlet]Unknown command.";
    }
}
