package com.example.model

enum class LightPowerState {
    UNKNOWN,
    ON,
    OFF,
}

enum class CloudStatus {
    CHECKING,
    ONLINE,
    OFFLINE,
}

data class LightCardConfig(
    val id: String,
    val name: String,
    val authToken: String,
    val virtualPin: String = "V0",
)

data class LightRuntimeState(
    val powerState: LightPowerState = LightPowerState.UNKNOWN,
    val isToggling: Boolean = false,
    val lastError: String? = null,
    val lastUpdatedMs: Long = 0L,
)

data class ThemeSettings(
    val backgroundImagePath: String? = null,
    val scale: Float = 1f,
    val offsetXFraction: Float = 0f,
    val offsetYFraction: Float = 0f,
    val topBlurDp: Float = 18f,
    val bottomBlurDp: Float = 18f,
    val blurColorArgb: Long = 0xFF0B0F17L,
    val overlayOpacity: Float = 0.35f,
)

data class AppSettings(
    val blynkServerUrl: String = DEFAULT_BLYNK_SERVER,
    val syncIntervalMs: Long = DEFAULT_SYNC_INTERVAL_MS,
    val tileAssignments: Map<Int, String> = emptyMap(),
    val theme: ThemeSettings = ThemeSettings(),
) {
    companion object {
        const val DEFAULT_BLYNK_SERVER = "https://blynk.cloud"
        const val MIN_SYNC_INTERVAL_MS = 100L
        const val MAX_SYNC_INTERVAL_MS = 5000L
        const val DEFAULT_SYNC_INTERVAL_MS = 1000L
        const val QUICK_TILE_COUNT = 8

        val BLYNK_REGION_PRESETS = listOf(
            "https://blynk.cloud",
            "https://ny3.blynk.cloud",
            "https://fra1.blynk.cloud",
            "https://lon1.blynk.cloud",
            "https://sgp1.blynk.cloud",
            "https://blr1.blynk.cloud",
        )
    }
}
