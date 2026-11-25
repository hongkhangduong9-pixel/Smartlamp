package com.example.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.BlynkLightApplication
import com.example.R
import com.example.model.LightPowerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

abstract class BaseLightTileService(
    private val slotIndex: Int,
) : TileService() {

    private var serviceScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        serviceScope = scope
        scope.launch {
            refreshTileAppearance()
        }
    }

    override fun onStopListening() {
        serviceScope?.cancel()
        serviceScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? BlynkLightApplication ?: return
        val scope = serviceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also {
            serviceScope = it
        }

        scope.launch {
            val light = app.settingsRepository.resolveLightForTileSlot(slotIndex)
            val tile = qsTile ?: return@launch

            if (light == null || light.authToken.isBlank()) {
                tile.label = light?.name ?: "Light $slotIndex"
                tile.state = Tile.STATE_UNAVAILABLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (light == null) "Unassigned" else "No token"
                }
                tile.updateTile()
                return@launch
            }

            tile.label = light.name
            tile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_qs_lightbulb)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Toggling..."
            }
            tile.updateTile()

            val result = app.blynkController.toggleLight(light.id)
            val updatedTile = qsTile ?: return@launch
            result.fold(
                onSuccess = { newState ->
                    updatedTile.label = light.name
                    updatedTile.state = if (newState == LightPowerState.ON) {
                        Tile.STATE_ACTIVE
                    } else {
                        Tile.STATE_INACTIVE
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        updatedTile.subtitle = if (newState == LightPowerState.ON) {
                            "ON • Tap to toggle"
                        } else {
                            "OFF • Tap to toggle"
                        }
                    }
                    updatedTile.updateTile()
                },
                onFailure = {
                    val preserved = app.blynkController.lightStates.value[light.id]?.powerState
                    updatedTile.label = light.name
                    updatedTile.state = if (preserved == LightPowerState.ON) {
                        Tile.STATE_ACTIVE
                    } else {
                        Tile.STATE_INACTIVE
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        updatedTile.subtitle = "Failed • Offline"
                    }
                    updatedTile.updateTile()
                },
            )
        }
    }

    private suspend fun refreshTileAppearance() {
        val app = applicationContext as? BlynkLightApplication ?: return
        val tile = qsTile ?: return
        val light = app.settingsRepository.resolveLightForTileSlot(slotIndex)

        tile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_qs_lightbulb)
        if (light == null || light.authToken.isBlank()) {
            tile.label = light?.name ?: "Light $slotIndex"
            tile.state = Tile.STATE_UNAVAILABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (light == null) "Unassigned" else "No token"
            }
            tile.updateTile()
            return
        }

        val cachedState = app.blynkController.lightStates.value[light.id]?.powerState
            ?: LightPowerState.UNKNOWN
        tile.label = light.name
        tile.state = if (cachedState == LightPowerState.ON) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (cachedState) {
                LightPowerState.ON -> "ON • Tap to toggle"
                LightPowerState.OFF -> "OFF • Tap to toggle"
                LightPowerState.UNKNOWN -> "Tap to toggle"
            }
        }
        tile.updateTile()
    }
}
