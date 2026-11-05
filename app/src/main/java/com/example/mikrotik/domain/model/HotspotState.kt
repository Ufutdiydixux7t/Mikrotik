package com.example.mikrotik.domain.model

sealed class HotspotState {
    object Disabled : HotspotState()
    object Enabling : HotspotState()
    data class Enabled(val interfaceName: String) : HotspotState()
    data class Error(val message: String) : HotspotState()
}
