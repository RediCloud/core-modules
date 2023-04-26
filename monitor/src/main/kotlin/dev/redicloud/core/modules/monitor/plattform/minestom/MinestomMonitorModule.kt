package dev.redicloud.core.modules.monitor.plattform.minestom

import dev.redicloud.api.ICoreAPI
import dev.redicloud.core.modules.monitor.MonitorModule
import dev.redicloud.core.modules.monitor.plattform.minestom.command.MinestomMonitorCommand

class MinestomMonitorModule : MonitorModule() {

    override val tpsMonitor: MinestomTPSMonitor = MinestomTPSMonitor()

    override fun onLoad(api: ICoreAPI) {

    }

    override fun onEnable(api: ICoreAPI) {
        api.commandManager.registerCommand(MinestomMonitorCommand())
    }

    override fun onDisable(api: ICoreAPI) {

    }

}