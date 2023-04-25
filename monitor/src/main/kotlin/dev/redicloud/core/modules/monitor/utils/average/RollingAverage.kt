package dev.redicloud.core.modules.monitor.utils.average

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayDeque

class RollingAverage(val size: Int) : IDoubleAverageInfo {

    private val samples: ArrayDeque<BigDecimal> = ArrayDeque(this.size + 1)
    private var total: BigDecimal = BigDecimal.ZERO

    fun getSimples(): Int =
        synchronized(this) {
            this.samples.size
        }

    fun add(value: BigDecimal) =
        synchronized(this) {
            this.total = this.total.add(value)
            this.samples.add(value)
            if (this.samples.size > this.size) {
                this.total = this.total.subtract(this.samples.removeFirst())
            }
        }

    override fun mean(): Double =
        synchronized(this) {
            if (this.samples.isEmpty()) {
                return 0.0
            }
            val divisor = BigDecimal.valueOf(this.samples.size.toDouble())
            this.total.divide(divisor, 30, RoundingMode.HALF_UP).toDouble()
        }

    override fun max(): Double =
        synchronized(this) {
            var max: BigDecimal? = null
            for (sample in this.samples) {
                if (max == null || sample > max) {
                    max = sample
                }
            }
            max?.toDouble() ?: 0.0
        }

    override fun min(): Double =
        synchronized(this) {
            var min: BigDecimal? = null
            for (sample in this.samples) {
                if (min == null || sample < min) {
                    min = sample
                }
            }
            min?.toDouble() ?: 0.0
        }

    override fun median(): Double =
        percentile(0.5)

    override fun percentile95(): Double =
        percentile(0.95)

    override fun percentile(percentile: Double): Double {
        if (percentile < 0 || percentile > 1) throw IllegalArgumentException("Percentile must be between 0 and 1")

        var sortedSamples: Array<BigDecimal>?
        synchronized(this) {
            if (this.samples.isEmpty()) {
                return 0.0
            }
            sortedSamples = this.samples.toTypedArray()
        }
        Arrays.sort(sortedSamples!!)

        val rank = Math.ceil(percentile * (sortedSamples!!.size - 1)).toInt()
        return sortedSamples!![rank].toDouble()
    }

}