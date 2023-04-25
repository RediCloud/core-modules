package dev.redicloud.core.modules.monitor.plattform.paper

import dev.redicloud.api.ICoreAPI
import dev.redicloud.core.modules.monitor.MonitorModule
import dev.redicloud.core.modules.monitor.tps.TPSMonitor

object PaperMonitorModule : MonitorModule() {

    override val tpsMonitor: TPSMonitor = PaperTPSMonitor()

    override fun onLoad(api: ICoreAPI) {

    }

    override fun onEnable(api: ICoreAPI) {

    }

    override fun onDisable(api: ICoreAPI) {

    }

}