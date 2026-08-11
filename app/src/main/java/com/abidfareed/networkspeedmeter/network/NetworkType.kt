package com.abidfareed.networkspeedmeter.network

/** Coarse active-network classification, derived from [android.net.NetworkCapabilities]. */
enum class NetworkType {
    WIFI,
    MOBILE,
    ETHERNET,
    VPN,
    OTHER,
    NONE
}
