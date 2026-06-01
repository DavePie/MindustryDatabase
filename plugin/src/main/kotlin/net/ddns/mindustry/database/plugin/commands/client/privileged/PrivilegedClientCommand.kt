package net.ddns.mindustry.database.plugin.commands.client.privileged

import arc.util.CommandHandler
import net.ddns.mindustry.database.plugin.commands.client.BaseClientCommand

sealed class PrivilegedClientCommand(handler: CommandHandler) : BaseClientCommand(handler)