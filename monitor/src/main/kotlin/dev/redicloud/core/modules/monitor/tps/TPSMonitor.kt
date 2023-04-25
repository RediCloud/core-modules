package dev.redicloud.core.modules.monitor.tps

abstract class TPSMonitor {

    abstract fun getTPS(type: TPSType): Double

    fun getTPSProgress(): Double =
        getTPS(TPSType.ONE_MINUTE) / 20.0f

    abstract fun getMSPT(): Double

    fun getMSPTProgress(): Double =
        getMSPT() / 50.0f


    private fun ensureProgressRange(progress: Double): Double =
        0.00.coerceAtLeast(1.00.coerceAtMost(progress))

    enum class TPSType {
        ONE_MINUTE,
        FIVE_MINUTES,
        FIFTEEN_MINUTES
    }

}