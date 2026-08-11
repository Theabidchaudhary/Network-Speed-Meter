package com.abidfareed.networkspeedmeter.util

import com.abidfareed.networkspeedmeter.settings.SpeedUnit
import java.util.Locale
import kotlin.math.pow

/** Formats a bytes/sec rate into a compact, unit-labeled string using binary (1024) units. */
object SpeedFormatter {

    private const val STEP = 1024.0
    private val UNIT_LABELS = listOf("B/s", "KB/s", "MB/s", "GB/s")

    fun format(bytesPerSec: Long, unit: SpeedUnit, decimalPlaces: Int): String {
        val safeDecimals = decimalPlaces.coerceIn(0, 2)
        val value = bytesPerSec.coerceAtLeast(0).toDouble()

        val (magnitude, label) = if (unit == SpeedUnit.AUTO) {
            autoScale(value)
        } else {
            val index = when (unit) {
                SpeedUnit.B -> 0
                SpeedUnit.KB -> 1
                SpeedUnit.MB -> 2
                SpeedUnit.GB -> 3
                SpeedUnit.AUTO -> 0
            }
            (value / STEP.pow(index)) to UNIT_LABELS[index]
        }

        return String.format(Locale.US, "%.${safeDecimals}f %s", magnitude, label)
    }

    /** Same as [format] but without the unit suffix, for compact status-bar icon rendering. */
    fun formatCompact(bytesPerSec: Long, unit: SpeedUnit, decimalPlaces: Int): Pair<String, String> {
        val safeDecimals = decimalPlaces.coerceIn(0, 2)
        val value = bytesPerSec.coerceAtLeast(0).toDouble()

        val (magnitude, label) = if (unit == SpeedUnit.AUTO) {
            autoScale(value)
        } else {
            val index = when (unit) {
                SpeedUnit.B -> 0
                SpeedUnit.KB -> 1
                SpeedUnit.MB -> 2
                SpeedUnit.GB -> 3
                SpeedUnit.AUTO -> 0
            }
            (value / STEP.pow(index)) to UNIT_LABELS[index]
        }

        val shortLabel = label.removeSuffix("/s")
        return String.format(Locale.US, "%.${safeDecimals}f", magnitude) to shortLabel
    }

    private fun autoScale(bytesPerSec: Double): Pair<Double, String> {
        var value = bytesPerSec
        var index = 0
        while (value >= STEP && index < UNIT_LABELS.lastIndex) {
            value /= STEP
            index++
        }
        return value to UNIT_LABELS[index]
    }
}
