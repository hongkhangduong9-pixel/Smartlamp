package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * High-performance Blynk HTTP API client.
 * Uses persistent HTTP/2 connection pooling and aggressive timeouts
 * to deliver ultra-low response latency for Quick Settings Tiles.
 */
class BlynkApiClient private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: BlynkApiClient? = null

        fun getInstance(): BlynkApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlynkApiClient().also { INSTANCE = it }
            }
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        // Aggressive timeouts optimized for fast remote switch actions
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .build()

    sealed class ApiResult<out T> {
        data class Success<out T>(val data: T, val latencyMs: Long = 0) : ApiResult<T>()
        data class Error(val message: String, val isTokenError: Boolean = false) : ApiResult<Nothing>()
    }

    /**
     * Updates a virtual pin value on Blynk Cloud (e.g. v0=1 or v1=0).
     * Endpoint: GET https://{server}/external/api/update?token={token}&{pin}={value}
     */
    suspend fun updatePin(
        server: String,
        token: String,
        pin: String,
        value: Int
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext ApiResult.Error("Chưa nhập Auth Token của Blynk", isTokenError = true)
        }

        val startTime = System.currentTimeMillis()
        val normalizedPin = pin.lowercase().trim()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host(server.trim())
            .addPathSegments("external/api/update")
            .addQueryParameter("token", token.trim())
            .addQueryParameter(normalizedPin, value.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    ApiResult.Success(Unit, latency)
                } else {
                    val body = response.body?.string().orEmpty()
                    val errorMessage = parseErrorMessage(response.code, body)
                    val isTokenError = response.code == 400 && errorMessage.contains("token", ignoreCase = true)
                    ApiResult.Error(errorMessage, isTokenError)
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("Lỗi kết nối mạng: ${e.localizedMessage ?: "Timeout"}")
        } catch (e: Exception) {
            ApiResult.Error("Lỗi không xác định: ${e.localizedMessage ?: "Error"}")
        }
    }

    /**
     * Retrieves the current value of a virtual pin from Blynk Cloud.
     * Endpoint: GET https://{server}/external/api/get?token={token}&{pin}
     */
    suspend fun getPin(
        server: String,
        token: String,
        pin: String
    ): ApiResult<Int> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext ApiResult.Error("Chưa nhập Auth Token của Blynk", isTokenError = true)
        }

        val startTime = System.currentTimeMillis()
        val normalizedPin = pin.lowercase().trim()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host(server.trim())
            .addPathSegments("external/api/get")
            .addQueryParameter("token", token.trim())
            .addQueryParameter(normalizedPin, "")
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty().trim()
                if (response.isSuccessful) {
                    val intVal = body.toIntOrNull()
                        ?: if (body.equals("true", ignoreCase = true)) 1 else 0
                    ApiResult.Success(intVal, latency)
                } else {
                    val errorMessage = parseErrorMessage(response.code, body)
                    val isTokenError = response.code == 400 && errorMessage.contains("token", ignoreCase = true)
                    ApiResult.Error(errorMessage, isTokenError)
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("Lỗi kết nối mạng: ${e.localizedMessage ?: "Timeout"}")
        } catch (e: Exception) {
            ApiResult.Error("Lỗi: ${e.localizedMessage ?: "Error"}")
        }
    }

    /**
     * Checks if the ESP32 hardware is currently connected to Blynk Cloud.
     * Endpoint: GET https://{server}/external/api/isHardwareConnected?token={token}
     */
    suspend fun isHardwareConnected(
        server: String,
        token: String
    ): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext ApiResult.Error("Chưa nhập Auth Token của Blynk", isTokenError = true)
        }

        val startTime = System.currentTimeMillis()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host(server.trim())
            .addPathSegments("external/api/isHardwareConnected")
            .addQueryParameter("token", token.trim())
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty().trim()
                if (response.isSuccessful) {
                    val isConnected = body.equals("true", ignoreCase = true)
                    ApiResult.Success(isConnected, latency)
                } else {
                    val errorMessage = parseErrorMessage(response.code, body)
                    val isTokenError = response.code == 400 && errorMessage.contains("token", ignoreCase = true)
                    ApiResult.Error(errorMessage, isTokenError)
                }
            }
        } catch (e: IOException) {
            ApiResult.Error("Lỗi kết nối mạng: ${e.localizedMessage ?: "Timeout"}")
        } catch (e: Exception) {
            ApiResult.Error("Lỗi: ${e.localizedMessage ?: "Error"}")
        }
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                errorObj.optString("message", "Mã lỗi $code")
            } else if (json.has("message")) {
                json.getString("message")
            } else {
                "Mã phản hồi: $code"
            }
        } catch (_: Exception) {
            if (body.isNotBlank() && body.length < 100) body else "Mã phản hồi HTTP $code"
        }
    }
}
