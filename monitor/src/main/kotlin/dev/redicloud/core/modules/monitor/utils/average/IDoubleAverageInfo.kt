package dev.redicloud.core.modules.monitor.utils.average

interface IDoubleAverageInfo {

    fun mean(): Double
    fun max(): Double
    fun min(): Double
    fun median(): Double
    fun percentile95(): Double
    fun percentile(percentile: Double): Double

}