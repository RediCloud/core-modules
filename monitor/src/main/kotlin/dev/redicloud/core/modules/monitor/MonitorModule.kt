package dev.redicloud.core.modules.monitor

import dev.redicloud.api.module.Module
import dev.redicloud.core.modules.monitor.cpu.CPUMonitor
import dev.redicloud.core.modules.monitor.tps.TPSMonitor

abstract class MonitorModule : Module() {

    companion object {
        lateinit var INSTANCE: MonitorModule
    }

    init {
        INSTANCE = this
    }

    abstract val tpsMonitor: TPSMonitor
    val cpuMonitor: CPUMonitor = CPUMonitor()

}