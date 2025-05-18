package net.ddns.mindustry.database.plugin.commands.client.unprivileged

import arc.util.CommandHandler
import net.ddns.mindustry.database.plugin.commands.client.ClientCommand

sealed class UnprivilegedClientCommand(handler: CommandHandler) : ClientCommand(handler)
