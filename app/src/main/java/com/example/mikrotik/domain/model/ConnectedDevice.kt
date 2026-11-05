package com.example.mikrotik.domain.model

data class ConnectedDevice(
    val ipAddress: String,
    val macAddress: String,
    val connectionState: String,
    val deviceName: String? = null,
    val interfaceName: String = "wlan0"
)
