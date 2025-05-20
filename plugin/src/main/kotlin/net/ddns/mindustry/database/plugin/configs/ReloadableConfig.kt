package net.ddns.mindustry.database.plugin.configs

import kotlin.reflect.full.primaryConstructor

sealed class ReloadableConfig {
    companion object Configs {
        fun reloadConfigs() {
            val configs = ReloadableConfig::class.sealedSubclasses

            for (config in configs) {
                config.primaryConstructor!!.call()
            }
        }
    }

    init {
        reload()
    }

    abstract fun reload()
}