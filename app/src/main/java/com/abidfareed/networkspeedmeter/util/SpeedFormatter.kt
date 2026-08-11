package com.abidfareed.networkspeedmeter.util

import com.abidfareed.networkspeedmeter.settings.SpeedUnit
import java.util.Locale
import kotlin.math.pow

/**
 * Formats a bytes/sec rate into a compact, unit-labeled string using binary (1024) units.
 * The smallest unit ever shown is KB/s - raw bytes/s are never displayed, so a near-idle
 * connection reads as "0.0 KB/s" rather than "42 B/s".
 */
object SpeedFormatter {

    private const val STEP = 1024.0
    private val UNIT_LABELS = listOf("KB/s", "MB/s", "GB/s")

    fun format(bytesPerSec: Long, unit: SpeedUnit, decimalPlaces: Int): String {
        val (magnitude, label) = scale(bytesPerSec, unit)
        val safeDecimals = decimalPlaces.coerceIn(0, 2)
        return String.format(Locale.US, "%.${safeDecimals}f %s", magnitude, label)
    }

    /** Same as [format] but returns (value, unit-without-"/s") for compact icon rendering. */
    fun formatCompact(bytesPerSec: Long, unit: SpeedUnit, decimalPlaces: Int): Pair<String, String> {
        val (magnitude, label) = scale(bytesPerSec, unit)
        val safeDecimals = decimalPlaces.coerceIn(0, 2)
        return String.format(Locale.US, "%.${safeDecimals}f", magnitude) to label.removeSuffix("/s")
    }

    private fun scale(bytesPerSec: Long, unit: SpeedUnit): Pair<Double, String> {
        val kbPerSec = bytesPerSec.coerceAtLeast(0).toDouble() / STEP

        return if (unit == SpeedUnit.AUTO) {
            autoScale(kbPerSec)
        } else {
            val index = when (unit) {
                SpeedUnit.KB -> 0
                SpeedUnit.MB -> 1
                SpeedUnit.GB -> 2
                SpeedUnit.AUTO -> 0
            }
            (kbPerSec / STEP.pow(index)) to UNIT_LABELS[index]
        }
    }

    private fun autoScale(kbPerSec: Double): Pair<Double, String> {
        var value = kbPerSec
        var index = 0
        while (value >= STEP && index < UNIT_LABELS.lastIndex) {
            value /= STEP
            index++
        }
        return value to UNIT_LABELS[index]
    }
}
