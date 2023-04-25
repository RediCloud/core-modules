package dev.redicloud.core.modules.monitor.plattform.minestom.command

import dev.redicloud.api.command.annotations.CommandSubPath
import dev.redicloud.clients.minestom.command.MinestomCommand
import dev.redicloud.core.modules.monitor.MonitorModule
import net.minestom.server.entity.Player

class MinestomMonitorCommand : MinestomCommand(
    "monitor",
    emptyArray(),
    "The monitor command for the minestom plattform",
    "system.monitor"
) {

    @CommandSubPath("cpu process")
    fun cpuProcess(player: Player) {
        player.sendMessage("Process-Load: ${MonitorModule.INSTANCE.cpuMonitor.getProcessLoad()}%/" +
                "${MonitorModule.INSTANCE.cpuMonitor.getProcessLoad10SecAvg()}%/" +
                "${MonitorModule.INSTANCE.cpuMonitor.getProcessLoad1MinAvg()}%/" +
                "${MonitorModule.INSTANCE.cpuMonitor.getProcessLoad15MinAvg()}%")
    }

    @CommandSubPath("cpu system")
    fun cpuSystem(player: Player) {
        player.sendMessage("System-Load: ${MonitorModule.INSTANCE.cpuMonitor.getSystemLoad()}%/" +
                "${MonitorModule.INSTANCE.cpuMonitor.getSystemLoad10SecAvg()}%/" +
                "${MonitorModule.INSTANCE.cpuMonitor.getSystemLoad1MinAvg()}%/" +
                "${MonitorModule.INSTANCE.cpuMonitor.getSystemLoad15MinAvg()}%")
    }

}