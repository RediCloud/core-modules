package dev.redicloud.core.modules.monitor.plattform.minestom

import dev.redicloud.core.modules.monitor.tps.TPSMonitor

class MinestomTPSMonitor : TPSMonitor() {

    override fun getTPS(type: TPSType): Double =
        0.0

    override fun getMSPT(): Double =
        0.0

}