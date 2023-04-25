package dev.redicloud.core.modules.monitor.plattform.velocity

import dev.redicloud.core.modules.monitor.tps.TPSMonitor

class VelocityTPSMonitor : TPSMonitor() {

    override fun getTPS(type: TPSType): Double =
        0.0

    override fun getMSPT(): Double =
        0.0

}