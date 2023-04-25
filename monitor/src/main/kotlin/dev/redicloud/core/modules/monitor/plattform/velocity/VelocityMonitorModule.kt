package dev.redicloud.core.modules.monitor.plattform.velocity

import dev.redicloud.api.ICoreAPI
import dev.redicloud.core.modules.monitor.MonitorModule

class VelocityMonitorModule : MonitorModule() {

    override val tpsMonitor: VelocityTPSMonitor = VelocityTPSMonitor()

    override fun onLoad(api: ICoreAPI) {

    }

    override fun onEnable(api: ICoreAPI) {

    }

    override fun onDisable(api: ICoreAPI) {

    }

}