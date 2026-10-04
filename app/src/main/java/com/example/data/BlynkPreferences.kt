package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blynk_settings")

/**
 * Manages configuration, virtual pin mappings, theme background customizations,
 * auto-sync for physical switches, and custom uploaded app icon selections.
 */
class BlynkPreferences(private val context: Context) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("blynk_auth_token")
        private val KEY_SERVER_HOST = stringPreferencesKey("blynk_server_host")
        private val KEY_TEMPLATE_ID = stringPreferencesKey("blynk_template_id")
        private val KEY_TEMPLATE_NAME = stringPreferencesKey("blynk_template_name")

        // Virtual Pin Configuration
        private val KEY_LIGHT1_PIN = stringPreferencesKey("blynk_light1_pin")
        private val KEY_LIGHT2_PIN = stringPreferencesKey("blynk_light2_pin")
        private val KEY_LIGHT1_NAME = stringPreferencesKey("blynk_light1_name")
        private val KEY_LIGHT2_NAME = stringPreferencesKey("blynk_light2_name")

        // Cached States
        private val KEY_LIGHT1_STATE = booleanPreferencesKey("blynk_light1_state")
        private val KEY_LIGHT2_STATE = booleanPreferencesKey("blynk_light2_state")

        // Background & Theme customization
        private val KEY_BG_IMAGE_URI = stringPreferencesKey("blynk_bg_image_uri")
        private val KEY_BG_BLUR_FRACTION = floatPreferencesKey("blynk_bg_blur_fraction")
        private val KEY_BG_CROP_SCALE = floatPreferencesKey("blynk_bg_crop_scale")
        private val KEY_BG_CROP_OFFSET_Y = floatPreferencesKey("blynk_bg_crop_offset_y")
        private val KEY_BG_ASPECT_RATIO = stringPreferencesKey("blynk_bg_aspect_ratio")
        private val KEY_THEME_MODE = stringPreferencesKey("blynk_theme_mode")

        // Physical switches & Auto-sync
        private val KEY_AUTO_SYNC_ENABLED = booleanPreferencesKey("blynk_auto_sync_enabled")
        private val KEY_AUTO_SYNC_INTERVAL_MS = intPreferencesKey("blynk_auto_sync_interval_ms")
        private val KEY_NOTIFY_PHYSICAL_SWITCH = booleanPreferencesKey("blynk_notify_physical_switch")

        // Custom App Icon Upload
        private val KEY_CUSTOM_ICON_URI = stringPreferencesKey("blynk_custom_icon_uri")
        private val KEY_ACTIVE_APP_ICON = stringPreferencesKey("blynk_active_app_icon")

        const val DEFAULT_SERVER = "blynk.cloud"
        const val DEFAULT_TEMPLATE_ID = "TMPL6mWdFodq6"
        const val DEFAULT_TEMPLATE_NAME = "Đèn Thông Minh"
        const val DEFAULT_PIN_1 = "v0"
        const val DEFAULT_PIN_2 = "v1"
        const val DEFAULT_NAME_1 = "Đèn 1"
        const val DEFAULT_NAME_2 = "Đèn 2"
        const val DEFAULT_BLUR_FRACTION = 0.75f // 3/4 of image height
        const val DEFAULT_AUTO_SYNC_INTERVAL_MS = 300 // 300ms (0.3s)
        const val DEFAULT_ASPECT_RATIO = "cover"

        @Volatile
        private var INSTANCE: BlynkPreferences? = null

        fun getInstance(context: Context): BlynkPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlynkPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val authTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTH_TOKEN] ?: ""
    }

    val serverHostFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SERVER_HOST] ?: DEFAULT_SERVER
    }

    val templateIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TEMPLATE_ID] ?: DEFAULT_TEMPLATE_ID
    }

    val templateNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TEMPLATE_NAME] ?: DEFAULT_TEMPLATE_NAME
    }

    val light1PinFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIGHT1_PIN] ?: DEFAULT_PIN_1
    }

    val light2PinFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIGHT2_PIN] ?: DEFAULT_PIN_2
    }

    val light1NameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIGHT1_NAME] ?: DEFAULT_NAME_1
    }

    val light2NameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIGHT2_NAME] ?: DEFAULT_NAME_2
    }

    val light1StateFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIGHT1_STATE] ?: false
    }

    val light2StateFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIGHT2_STATE] ?: false
    }

    val bgImageUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BG_IMAGE_URI] ?: ""
    }

    val bgBlurFractionFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_BG_BLUR_FRACTION] ?: DEFAULT_BLUR_FRACTION
    }

    val bgCropScaleFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_BG_CROP_SCALE] ?: 1.0f
    }

    val bgCropOffsetYFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_BG_CROP_OFFSET_Y] ?: 0f
    }

    val bgAspectRatioFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BG_ASPECT_RATIO] ?: DEFAULT_ASPECT_RATIO
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "dark"
    }

    val autoSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SYNC_ENABLED] ?: true
    }

    val autoSyncIntervalMsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_SYNC_INTERVAL_MS] ?: DEFAULT_AUTO_SYNC_INTERVAL_MS
    }

    val notifyPhysicalSwitchFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFY_PHYSICAL_SWITCH] ?: true
    }

    val customIconUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_ICON_URI] ?: ""
    }

    val activeAppIconFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_APP_ICON] ?: "default"
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token.trim()
        }
    }

    suspend fun saveServerHost(server: String) {
        val cleanServer = server.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_HOST] = if (cleanServer.isNotBlank()) cleanServer else DEFAULT_SERVER
        }
    }

    suspend fun saveTemplateInfo(templateId: String, templateName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TEMPLATE_ID] = templateId.trim()
            preferences[KEY_TEMPLATE_NAME] = templateName.trim()
        }
    }

    suspend fun saveVirtualPins(pin1: String, name1: String, pin2: String, name2: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LIGHT1_PIN] = pin1.lowercase().trim()
            preferences[KEY_LIGHT1_NAME] = name1.trim()
            preferences[KEY_LIGHT2_PIN] = pin2.lowercase().trim()
            preferences[KEY_LIGHT2_NAME] = name2.trim()
        }
    }

    suspend fun saveLightState(lightIndex: Int, isOn: Boolean) {
        context.dataStore.edit { preferences ->
            if (lightIndex == 1) {
                preferences[KEY_LIGHT1_STATE] = isOn
            } else {
                preferences[KEY_LIGHT2_STATE] = isOn
            }
        }
    }

    suspend fun saveBackgroundSettings(imageUri: String, blurFraction: Float, cropScale: Float, cropOffsetY: Float, aspectRatio: String = DEFAULT_ASPECT_RATIO) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BG_IMAGE_URI] = imageUri
            preferences[KEY_BG_BLUR_FRACTION] = blurFraction
            preferences[KEY_BG_CROP_SCALE] = cropScale
            preferences[KEY_BG_CROP_OFFSET_Y] = cropOffsetY
            preferences[KEY_BG_ASPECT_RATIO] = aspectRatio
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun saveAutoSyncSettings(enabled: Boolean, intervalMs: Int, notify: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SYNC_ENABLED] = enabled
            preferences[KEY_AUTO_SYNC_INTERVAL_MS] = intervalMs
            preferences[KEY_NOTIFY_PHYSICAL_SWITCH] = notify
        }
    }

    suspend fun saveCustomIconUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_ICON_URI] = uri
        }
    }

    suspend fun saveActiveAppIcon(iconKey: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_APP_ICON] = iconKey
        }
    }

    suspend fun getAuthToken(): String = context.dataStore.data.first()[KEY_AUTH_TOKEN] ?: ""
    suspend fun getServerHost(): String = context.dataStore.data.first()[KEY_SERVER_HOST] ?: DEFAULT_SERVER
    suspend fun getLight1Pin(): String = context.dataStore.data.first()[KEY_LIGHT1_PIN] ?: DEFAULT_PIN_1
    suspend fun getLight2Pin(): String = context.dataStore.data.first()[KEY_LIGHT2_PIN] ?: DEFAULT_PIN_2
    suspend fun getLight1Name(): String = context.dataStore.data.first()[KEY_LIGHT1_NAME] ?: DEFAULT_NAME_1
    suspend fun getLight2Name(): String = context.dataStore.data.first()[KEY_LIGHT2_NAME] ?: DEFAULT_NAME_2

    suspend fun getCachedLightState(lightIndex: Int): Boolean {
        val prefs = context.dataStore.data.first()
        return if (lightIndex == 1) prefs[KEY_LIGHT1_STATE] ?: false else prefs[KEY_LIGHT2_STATE] ?: false
    }

    fun getCachedLightStateSync(lightIndex: Int): Boolean {
        return try {
            runBlocking { getCachedLightState(lightIndex) }
        } catch (_: Exception) {
            false
        }
    }

    fun getLightNameSync(lightIndex: Int): String {
        return try {
            runBlocking { if (lightIndex == 1) getLight1Name() else getLight2Name() }
        } catch (_: Exception) {
            if (lightIndex == 1) DEFAULT_NAME_1 else DEFAULT_NAME_2
        }
    }

    fun getLightPinSync(lightIndex: Int): String {
        return try {
            runBlocking { if (lightIndex == 1) getLight1Pin() else getLight2Pin() }
        } catch (_: Exception) {
            if (lightIndex == 1) DEFAULT_PIN_1 else DEFAULT_PIN_2
        }
    }
}
