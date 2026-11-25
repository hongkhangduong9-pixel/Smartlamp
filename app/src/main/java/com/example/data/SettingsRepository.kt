package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.LightCardConfig
import com.example.model.ThemeSettings
import com.example.network.BlynkApiClient
import com.example.security.CredentialCryptoManager
import com.example.security.TokenCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val Context.blynkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "blynk_light_settings",
)

class SettingsRepository(
    private val context: Context,
    private val cipher: TokenCipher = CredentialCryptoManager(),
) {
    private val dataStore = context.applicationContext.blynkDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val lightsFlow: StateFlow<List<LightCardConfig>> = dataStore.data
        .map { prefs -> decodeLights(prefs[KEY_LIGHTS_JSON]) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val settingsFlow: StateFlow<AppSettings> = dataStore.data
        .map { prefs -> decodeSettings(prefs) }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun getLightsSnapshot(): List<LightCardConfig> {
        return decodeLights(dataStore.data.first()[KEY_LIGHTS_JSON])
    }

    suspend fun getSettingsSnapshot(): AppSettings {
        return decodeSettings(dataStore.data.first())
    }

    suspend fun addLight(name: String, authToken: String, virtualPin: String): LightCardConfig {
        val newCard = LightCardConfig(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Light" },
            authToken = authToken.trim(),
            virtualPin = BlynkApiClient.formatPinForDisplay(virtualPin),
        )
        dataStore.edit { prefs ->
            val current = decodeLights(prefs[KEY_LIGHTS_JSON]).toMutableList()
            current.add(newCard)
            prefs[KEY_LIGHTS_JSON] = encodeLights(current)
        }
        return newCard
    }

    suspend fun updateLight(updated: LightCardConfig) {
        val sanitized = updated.copy(
            name = updated.name.trim().ifEmpty { "Light" },
            authToken = updated.authToken.trim(),
            virtualPin = BlynkApiClient.formatPinForDisplay(updated.virtualPin),
        )
        dataStore.edit { prefs ->
            val current = decodeLights(prefs[KEY_LIGHTS_JSON]).map {
                if (it.id == sanitized.id) sanitized else it
            }
            prefs[KEY_LIGHTS_JSON] = encodeLights(current)
        }
    }

    suspend fun deleteLight(lightId: String) {
        dataStore.edit { prefs ->
            val current = decodeLights(prefs[KEY_LIGHTS_JSON]).filterNot { it.id == lightId }
            prefs[KEY_LIGHTS_JSON] = encodeLights(current)

            val assignments = decodeTileAssignments(prefs[KEY_TILE_ASSIGNMENTS_JSON])
                .filterValues { it != lightId }
            prefs[KEY_TILE_ASSIGNMENTS_JSON] = encodeTileAssignments(assignments)
        }
    }

    suspend fun updateServerUrl(rawUrl: String) {
        val normalized = BlynkApiClient.normalizeServerUrl(rawUrl) ?: AppSettings.DEFAULT_BLYNK_SERVER
        dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalized
        }
    }

    suspend fun updateSyncIntervalMs(intervalMs: Long) {
        val clamped = intervalMs.coerceIn(
            AppSettings.MIN_SYNC_INTERVAL_MS,
            AppSettings.MAX_SYNC_INTERVAL_MS,
        )
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_INTERVAL_MS] = clamped
        }
    }

    suspend fun updateTileAssignment(slotIndex: Int, lightId: String?) {
        dataStore.edit { prefs ->
            val current = decodeTileAssignments(prefs[KEY_TILE_ASSIGNMENTS_JSON]).toMutableMap()
            if (lightId.isNullOrBlank()) {
                current.remove(slotIndex)
            } else {
                current[slotIndex] = lightId
            }
            prefs[KEY_TILE_ASSIGNMENTS_JSON] = encodeTileAssignments(current)
        }
    }

    suspend fun resolveLightForTileSlot(slotIndex: Int): LightCardConfig? {
        val lights = getLightsSnapshot()
        if (lights.isEmpty()) return null
        val assignments = getSettingsSnapshot().tileAssignments
        val explicitId = assignments[slotIndex]
        if (!explicitId.isNullOrBlank()) {
            return lights.firstOrNull { it.id == explicitId }
        }
        // Fallback auto-assignment for convenience: Slot 1 -> Light 0, Slot 2 -> Light 1, etc.
        return lights.getOrNull(slotIndex - 1)
    }

    suspend fun updateThemeSettings(transform: (ThemeSettings) -> ThemeSettings) {
        dataStore.edit { prefs ->
            val current = decodeTheme(prefs)
            val next = transform(current)
            if (next.backgroundImagePath != null) {
                prefs[KEY_BG_PATH] = next.backgroundImagePath
            } else {
                prefs.remove(KEY_BG_PATH)
            }
            prefs[KEY_BG_SCALE] = next.scale.coerceIn(1f, 4f)
            prefs[KEY_BG_OFFSET_X] = next.offsetXFraction.coerceIn(-1f, 1f)
            prefs[KEY_BG_OFFSET_Y] = next.offsetYFraction.coerceIn(-1f, 1f)
            prefs[KEY_TOP_BLUR_DP] = next.topBlurDp.coerceIn(0f, 40f)
            prefs[KEY_BOTTOM_BLUR_DP] = next.bottomBlurDp.coerceIn(0f, 40f)
            prefs[KEY_BLUR_COLOR] = next.blurColorArgb
            prefs[KEY_OVERLAY_OPACITY] = next.overlayOpacity.coerceIn(0f, 0.85f)
        }
    }

    suspend fun saveBackgroundFromUri(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeScaledBitmap(sourceUri, maxDimension = 2048)
                ?: return@withContext Result.failure(IllegalArgumentException("Could not decode selected image"))

            val bgDir = File(context.applicationContext.filesDir, "backgrounds")
            if (!bgDir.exists()) bgDir.mkdirs()

            // Remove old background files to prevent storage bloat
            bgDir.listFiles()?.forEach { old ->
                if (old.name.startsWith("home_bg_")) old.delete()
            }

            val outFile = File(bgDir, "home_bg_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            bitmap.recycle()

            updateThemeSettings { current ->
                current.copy(
                    backgroundImagePath = outFile.absolutePath,
                    scale = 1f,
                    offsetXFraction = 0f,
                    offsetYFraction = 0f,
                )
            }
            Result.success(outFile.absolutePath)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun clearBackgroundImage() = withContext(Dispatchers.IO) {
        val bgDir = File(context.applicationContext.filesDir, "backgrounds")
        bgDir.listFiles()?.forEach { it.delete() }
        updateThemeSettings { current ->
            current.copy(
                backgroundImagePath = null,
                scale = 1f,
                offsetXFraction = 0f,
                offsetYFraction = 0f,
            )
        }
    }

    private fun decodeScaledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val resolver = context.applicationContext.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = maxOf(w, h)
                if (largest > maxDimension) {
                    val ratio = maxDimension.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (w * ratio).toInt().coerceAtLeast(1),
                        (h * ratio).toInt().coerceAtLeast(1),
                    )
                }
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    private fun decodeSettings(prefs: Preferences): AppSettings {
        val server = prefs[KEY_SERVER_URL]?.takeIf { it.isNotBlank() }
            ?: AppSettings.DEFAULT_BLYNK_SERVER
        val interval = (prefs[KEY_SYNC_INTERVAL_MS] ?: AppSettings.DEFAULT_SYNC_INTERVAL_MS)
            .coerceIn(AppSettings.MIN_SYNC_INTERVAL_MS, AppSettings.MAX_SYNC_INTERVAL_MS)
        val assignments = decodeTileAssignments(prefs[KEY_TILE_ASSIGNMENTS_JSON])
        return AppSettings(
            blynkServerUrl = server,
            syncIntervalMs = interval,
            tileAssignments = assignments,
            theme = decodeTheme(prefs),
        )
    }

    private fun decodeTheme(prefs: Preferences): ThemeSettings {
        val path = prefs[KEY_BG_PATH]?.takeIf { File(it).exists() }
        return ThemeSettings(
            backgroundImagePath = path,
            scale = prefs[KEY_BG_SCALE] ?: 1f,
            offsetXFraction = prefs[KEY_BG_OFFSET_X] ?: 0f,
            offsetYFraction = prefs[KEY_BG_OFFSET_Y] ?: 0f,
            topBlurDp = prefs[KEY_TOP_BLUR_DP] ?: 18f,
            bottomBlurDp = prefs[KEY_BOTTOM_BLUR_DP] ?: 18f,
            blurColorArgb = prefs[KEY_BLUR_COLOR] ?: 0xFF0B0F17L,
            overlayOpacity = prefs[KEY_OVERLAY_OPACITY] ?: 0.35f,
        )
    }

    private fun encodeLights(lights: List<LightCardConfig>): String {
        val array = JSONArray()
        for (light in lights) {
            val obj = JSONObject()
            obj.put("id", light.id)
            obj.put("name", light.name)
            obj.put("token", cipher.encrypt(light.authToken))
            obj.put("pin", light.virtualPin)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeLights(rawJson: String?): List<LightCardConfig> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(rawJson)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name", "Light")
                    val encryptedToken = obj.optString("token", "")
                    val decryptedToken = cipher.decrypt(encryptedToken)
                    val pin = obj.optString("pin", "V0")
                    add(
                        LightCardConfig(
                            id = id,
                            name = name,
                            authToken = decryptedToken,
                            virtualPin = BlynkApiClient.formatPinForDisplay(pin),
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun encodeTileAssignments(map: Map<Int, String>): String {
        val obj = JSONObject()
        map.forEach { (slot, lightId) ->
            obj.put(slot.toString(), lightId)
        }
        return obj.toString()
    }

    private fun decodeTileAssignments(rawJson: String?): Map<Int, String> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(rawJson)
            buildMap {
                for (key in obj.keys()) {
                    val slot = key.toIntOrNull() ?: continue
                    val lightId = obj.optString(key).takeIf { it.isNotBlank() } ?: continue
                    put(slot, lightId)
                }
            }
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    companion object {
        private val KEY_LIGHTS_JSON = stringPreferencesKey("lights_json")
        private val KEY_SERVER_URL = stringPreferencesKey("blynk_server_url")
        private val KEY_SYNC_INTERVAL_MS = longPreferencesKey("sync_interval_ms")
        private val KEY_TILE_ASSIGNMENTS_JSON = stringPreferencesKey("tile_assignments_json")
        private val KEY_BG_PATH = stringPreferencesKey("theme_bg_path")
        private val KEY_BG_SCALE = floatPreferencesKey("theme_bg_scale")
        private val KEY_BG_OFFSET_X = floatPreferencesKey("theme_bg_offset_x")
        private val KEY_BG_OFFSET_Y = floatPreferencesKey("theme_bg_offset_y")
        private val KEY_TOP_BLUR_DP = floatPreferencesKey("theme_top_blur_dp")
        private val KEY_BOTTOM_BLUR_DP = floatPreferencesKey("theme_bottom_blur_dp")
        private val KEY_BLUR_COLOR = longPreferencesKey("theme_blur_color")
        private val KEY_OVERLAY_OPACITY = floatPreferencesKey("theme_overlay_opacity")
    }
}
