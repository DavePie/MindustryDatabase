package net.ddns.mindustry.database.plugin.commands.server

import arc.util.CommandHandler
import net.ddns.mindustry.database.plugin.commands.BaseCommand

sealed class BaseServerCommand(handler: CommandHandler) : BaseCommand(handler)