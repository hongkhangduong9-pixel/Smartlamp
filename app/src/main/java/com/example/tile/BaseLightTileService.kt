package com.example.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.R
import com.example.data.BlynkPreferences
import com.example.data.BlynkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base TileService for controlling ESP32 smart lights directly from Android Control Center / Quick Settings.
 * - Zero activity launch overhead
 * - Instant optimistic UI state change
 * - Dynamically reflects custom Pin and Name configured in settings
 * - Automatic rollback & user notification on failure
 */
abstract class BaseLightTileService : TileService() {

    abstract val lightIndex: Int

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: BlynkRepository
    private lateinit var preferences: BlynkPreferences

    override fun onCreate() {
        super.onCreate()
        repository = BlynkRepository.getInstance(applicationContext)
        preferences = BlynkPreferences.getInstance(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileUiFromCache()
    }

    private fun updateTileUiFromCache() {
        val tile = qsTile ?: return
        val isLightOn = preferences.getCachedLightStateSync(lightIndex)
        val name = preferences.getLightNameSync(lightIndex)

        tile.icon = Icon.createWithResource(this, R.drawable.ic_lightbulb)
        tile.label = name
        tile.state = if (isLightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isLightOn) getString(R.string.state_on) else getString(R.string.state_off)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        serviceScope.launch {
            val token = preferences.getAuthToken()
            if (token.isBlank()) {
                showToast(getString(R.string.error_no_token))
                return@launch
            }

            // 1. Instant Optimistic State Flip for zero perceived latency
            val wasOn = (tile.state == Tile.STATE_ACTIVE)
            val willBeOn = !wasOn

            tile.state = if (willBeOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (willBeOn) getString(R.string.state_toggling_on) else getString(R.string.state_toggling_off)
            }
            tile.updateTile()

            // 2. Dispatch Background Request
            withContext(Dispatchers.IO) {
                val result = repository.setLightState(lightIndex, willBeOn)
                withContext(Dispatchers.Main) {
                    val currentTile = qsTile ?: return@withContext
                    val name = preferences.getLightNameSync(lightIndex)
                    if (result.isSuccess) {
                        currentTile.state = if (willBeOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            currentTile.subtitle = if (willBeOn) getString(R.string.state_on) else getString(R.string.state_off)
                        }
                        currentTile.updateTile()
                    } else {
                        // Rollback on failure
                        currentTile.state = if (wasOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            currentTile.subtitle = getString(R.string.error_blynk)
                        }
                        currentTile.updateTile()

                        val errorMsg = result.exceptionOrNull()?.localizedMessage
                            ?: getString(R.string.error_network)
                        showToast("$name: $errorMsg")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
