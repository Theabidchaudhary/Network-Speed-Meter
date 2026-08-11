package com.abidfareed.networkspeedmeter.network

import android.net.TrafficStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import android.os.SystemClock

/**
 * Samples cumulative device-wide RX/TX byte counters ([TrafficStats]) on a rolling interval and
 * turns the deltas into an instantaneous bytes/sec rate. This only reads counters that Android
 * already maintains for every app (no elevated permission, no traffic inspection).
 *
 * Note: [TrafficStats.getTotalRxBytes]/[getTotalTxBytes] sum every network interface since boot,
 * including a VPN's virtual `tun` interface *and* the physical interface carrying its encrypted
 * payload. When a VPN is active this can slightly overcount instantaneous throughput — a known,
 * documented Android platform limitation, not something this app can correct without deeper
 * per-UID accounting via NetworkStatsManager (which requires a user-granted special access
 * permission this project intentionally avoids requesting).
 */
class SpeedTracker {

    /**
     * Emits a new [SpeedSample] every [intervalMillis] value received from [intervalMillisFlow].
     * Changing the interval restarts the ticker at the new cadence.
     */
    fun samples(intervalMillisFlow: Flow<Long>): Flow<SpeedSample> =
        intervalMillisFlow.flatMapLatestTicker()

    private fun Flow<Long>.flatMapLatestTicker(): Flow<SpeedSample> =
        flatMapLatest { intervalMillis -> ticker(intervalMillis) }

    private fun ticker(intervalMillis: Long): Flow<SpeedSample> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastTime = SystemClock.elapsedRealtime()

        while (true) {
            delay(intervalMillis)

            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val now = SystemClock.elapsedRealtime()

            val elapsedSec = ((now - lastTime).coerceAtLeast(1)) / 1000.0
            val supported = rx != TrafficStats.UNSUPPORTED.toLong() && tx != TrafficStats.UNSUPPORTED.toLong()
            val down = if (supported && rx >= lastRx) ((rx - lastRx) / elapsedSec).toLong() else 0L
            val up = if (supported && tx >= lastTx) ((tx - lastTx) / elapsedSec).toLong() else 0L

            lastRx = rx
            lastTx = tx
            lastTime = now

            emit(SpeedSample(downloadBytesPerSec = down, uploadBytesPerSec = up))
        }
    }
}
