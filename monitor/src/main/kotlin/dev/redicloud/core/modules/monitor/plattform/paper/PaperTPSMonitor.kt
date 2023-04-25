package dev.redicloud.core.modules.monitor.plattform.paper

import dev.redicloud.core.modules.monitor.tps.TPSMonitor
import org.bukkit.Bukkit

class PaperTPSMonitor : TPSMonitor() {

    override fun getTPS(type: TPSType): Double =
        when(type) {
            TPSType.ONE_MINUTE -> Bukkit.getTPS()[0]
            TPSType.FIVE_MINUTES -> Bukkit.getTPS()[1]
            TPSType.FIFTEEN_MINUTES -> Bukkit.getTPS()[2]
        }

    override fun getMSPT(): Double =
        Bukkit.getAverageTickTime()

}