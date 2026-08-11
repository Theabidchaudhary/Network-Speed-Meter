package com.abidfareed.networkspeedmeter.network

/** Instantaneous throughput, in bytes per second, from the most recent sampling window. */
data class SpeedSample(
    val downloadBytesPerSec: Long,
    val uploadBytesPerSec: Long
)
