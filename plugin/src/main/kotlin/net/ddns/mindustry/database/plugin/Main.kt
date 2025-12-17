package net.ddns.mindustry.database.plugin

import arc.util.CommandHandler
import arc.util.Log
import mindustry.mod.Plugin
import net.ddns.mindustry.database.client.Database
import net.ddns.mindustry.database.plugin.commands.client.privileged.PrivilegedClientCommand
import net.ddns.mindustry.database.plugin.commands.client.unprivileged.UnprivilegedClientCommand
import net.ddns.mindustry.database.plugin.commands.server.ServerCommand
import net.ddns.mindustry.database.plugin.commands.server.permissions.BasePermissionCommand
import net.ddns.mindustry.database.plugin.commands.server.roles.BaseRoleCommand
import java.util.logging.LogManager

@Suppress("unused")
class Main : Plugin() {
    companion object {
        var database: Database? = null
        var unprivilegedClientCommands = UnprivilegedClientCommand::class.sealedSubclasses
        var privilegedClientCommands = PrivilegedClientCommand::class.sealedSubclasses

        var serverCommands = ServerCommand::class.sealedSubclasses
        var roleCommands = BaseRoleCommand::class.sealedSubclasses
        var permissionCommands = BasePermissionCommand::class.sealedSubclasses
    }

    override fun init() {
        // I'm going to kill myself if jOOQ sends another self-ad
        LogManager.getLogManager().reset()
        // https://stackoverflow.com/a/5762502
        Log.info("\u001B[34mPowered by jOOQ.\u001B[0m")

        loadMindustryEvents()
        loadChatFilters()
        loadActionFilters()
        restartConfigDependentFeatures()

        if (database == null) {
            Log.warn("Couldn't start database. Privileged commands won't be available until the server is properly" +
                    " configured and restarted.")
            return;
        }

        loadDatabaseEvents()

        Log.info("Database plugin loaded.")
        Log.warn("Do NOT run `exit` when hosting. Instead, stop the server with `stop` and then `exit`. This ensures " +
                "that the scheduler is closed gracefully.")
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerCommands(unprivilegedClientCommands, handler)

        if (database == null) return

        registerCommands(privilegedClientCommands, handler)
    }

    override fun registerServerCommands(handler: CommandHandler) {
        registerCommands(serverCommands, handler)
        registerCommands(roleCommands, handler)
        registerCommands(permissionCommands, handler)
    }
}