package dev.redicloud.core.modules.monitor.cpu

import dev.redicloud.core.modules.monitor.utils.average.RollingAverage
import kotlinx.coroutines.*
import java.lang.management.ManagementFactory
import java.math.BigDecimal
import javax.management.JMX
import javax.management.MBeanServer
import javax.management.ObjectName

class CPUMonitor {

    private val operatingSystemBeam = "java.lang:type=OperatingSystem"
    private val bean: OperatingSystemMXBean

    private val collectJob: Job
    private val collectScope = CoroutineScope(Dispatchers.Default)

    private val systemAverage10Sec = RollingAverage(10)
    private val systemAverage1Min = RollingAverage(60)
    private val systemAverage15Min = RollingAverage(60 * 15)
    private val processAverage10Sec = RollingAverage(10)
    private val processAverage1Min = RollingAverage(60)
    private val processAverage15Min = RollingAverage(60 * 15)

    init {
        try {
            val beanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            val diagnosticBeanName = ObjectName.getInstance(operatingSystemBeam)
            bean = JMX.newMBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean::class.java)
        }catch (e: Exception) {
            throw UnsupportedOperationException("OperatingSystemMXBean not supported!")
        }
        this.collectJob = startCollector()
    }

    fun getSystemLoad(): Double =
        this.bean.getSystemCpuLoad()

    fun getSystemLoad10SecAvg(): Double =
        this.systemAverage10Sec.mean()

    fun getSystemLoad1MinAvg(): Double =
        this.systemAverage1Min.mean()

    fun getSystemLoad15MinAvg(): Double =
        this.systemAverage15Min.mean()

    fun getProcessLoad(): Double =
        this.bean.getProcessCpuLoad()

    fun getProcessLoad10SecAvg(): Double =
        this.processAverage10Sec.mean()

    fun getProcessLoad1MinAvg(): Double =
        this.processAverage1Min.mean()

    fun getProcessLoad15MinAvg(): Double =
        this.processAverage15Min.mean()


    private fun startCollector(): Job =
        collectScope.launch {
            val systemAverages = arrayOf(
                systemAverage10Sec,
                systemAverage1Min,
                systemAverage15Min
            )
            val processAverages = arrayOf(
                processAverage10Sec,
                processAverage1Min,
                processAverage15Min
            )
            while (true) {
                delay(1000)

                val systemCpuLoad = BigDecimal(getSystemLoad())
                val processCpuLoad = BigDecimal(getProcessLoad())

                if (systemCpuLoad.signum() != -1) {
                    for (average in systemAverages) {
                        average.add(systemCpuLoad)
                    }
                }

                if (processCpuLoad.signum() != -1) {
                    for (average in processAverages) {
                        average.add(processCpuLoad)
                    }
                }
            }
        }

    interface OperatingSystemMXBean {
        fun getSystemCpuLoad(): Double
        fun getProcessCpuLoad(): Double
    }

}