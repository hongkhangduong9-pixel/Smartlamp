package com.example.network

import com.example.model.LightPowerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ConnectivityTestResult(
    val serverReachable: Boolean,
    val hardwareConnected: Boolean?,
    val message: String,
)

class BlynkApiException(
    message: String,
    val isConnectivityFailure: Boolean = false,
    val httpCode: Int? = null,
) : Exception(message)

interface BlynkApi {
    suspend fun readPin(serverUrl: String, authToken: String, virtualPin: String): Result<LightPowerState>
    suspend fun readMultiplePins(
        serverUrl: String,
        authToken: String,
        virtualPins: List<String>,
    ): Result<Map<String, LightPowerState>>
    suspend fun writePin(
        serverUrl: String,
        authToken: String,
        virtualPin: String,
        targetState: LightPowerState,
    ): Result<Unit>
    suspend fun toggleLight(
        serverUrl: String,
        authToken: String,
        virtualPin: String,
    ): Result<LightPowerState>
    suspend fun testConnectivity(serverUrl: String, sampleToken: String?): ConnectivityTestResult
}

class BlynkApiClient(
    private val client: OkHttpClient = createDefaultClient(),
) : BlynkApi {

    override suspend fun readPin(
        serverUrl: String,
        authToken: String,
        virtualPin: String,
    ): Result<LightPowerState> = withContext(Dispatchers.IO) {
        val base = normalizeServerUrl(serverUrl)
            ?: return@withContext Result.failure(BlynkApiException("Invalid Blynk server URL"))
        val token = authToken.trim()
        if (token.isEmpty()) {
            return@withContext Result.failure(BlynkApiException("Missing Blynk AuthToken"))
        }
        val pin = normalizePinForApi(virtualPin)
        val url = "$base/external/api/get".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("token", token)
            ?.addEncodedQueryParameter(pin, null)
            ?.build()
            ?: return@withContext Result.failure(BlynkApiException("Malformed Blynk URL"))

        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        BlynkApiException(
                            message = parseBlynkError(body, response.code),
                            isConnectivityFailure = response.code >= 500 || response.code == 429,
                            httpCode = response.code,
                        )
                    )
                }
                val parsed = parseSinglePinResponse(body)
                    ?: return@withContext Result.failure(
                        BlynkApiException("Unreadable pin response from Blynk")
                    )
                Result.success(parsed)
            }
        } catch (e: IOException) {
            Result.failure(
                BlynkApiException(
                    message = e.message ?: "Network unreachable",
                    isConnectivityFailure = true,
                )
            )
        }
    }

    override suspend fun readMultiplePins(
        serverUrl: String,
        authToken: String,
        virtualPins: List<String>,
    ): Result<Map<String, LightPowerState>> = withContext(Dispatchers.IO) {
        val normalizedPins = virtualPins.map { normalizePinForApi(it) }.distinct()
        if (normalizedPins.isEmpty()) return@withContext Result.success(emptyMap())
        if (normalizedPins.size == 1) {
            val singlePin = normalizedPins.first()
            return@withContext readPin(serverUrl, authToken, singlePin).map { state ->
                mapOf(singlePin to state)
            }
        }

        val base = normalizeServerUrl(serverUrl)
            ?: return@withContext Result.failure(BlynkApiException("Invalid Blynk server URL"))
        val token = authToken.trim()
        if (token.isEmpty()) {
            return@withContext Result.failure(BlynkApiException("Missing Blynk AuthToken"))
        }

        val urlBuilder = "$base/external/api/get".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("token", token)
            ?: return@withContext Result.failure(BlynkApiException("Malformed Blynk URL"))

        normalizedPins.forEach { pin ->
            urlBuilder.addEncodedQueryParameter(pin, null)
        }

        val request = Request.Builder().url(urlBuilder.build()).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // If HTTP 400 on batch (e.g., one datastream missing), fall back to single-pin reads
                    if (response.code == 400) {
                        return@withContext readPinsIndividually(serverUrl, token, normalizedPins)
                    }
                    return@withContext Result.failure(
                        BlynkApiException(
                            message = parseBlynkError(body, response.code),
                            isConnectivityFailure = response.code >= 500 || response.code == 429,
                            httpCode = response.code,
                        )
                    )
                }

                val parsedMap = parseMultiPinResponse(body, normalizedPins)
                if (parsedMap.isEmpty()) {
                    return@withContext Result.failure(
                        BlynkApiException("Unreadable batch response from Blynk")
                    )
                }
                Result.success(parsedMap)
            }
        } catch (e: IOException) {
            Result.failure(
                BlynkApiException(
                    message = e.message ?: "Network unreachable",
                    isConnectivityFailure = true,
                )
            )
        }
    }

    private suspend fun readPinsIndividually(
        serverUrl: String,
        token: String,
        normalizedPins: List<String>,
    ): Result<Map<String, LightPowerState>> {
        val results = mutableMapOf<String, LightPowerState>()
        var lastError: Throwable? = null
        for (pin in normalizedPins) {
            val res = readPin(serverUrl, token, pin)
            res.onSuccess { results[pin] = it }
                .onFailure { lastError = it }
        }
        return if (results.isNotEmpty()) {
            Result.success(results)
        } else {
            Result.failure(lastError ?: BlynkApiException("Failed to read pins"))
        }
    }

    override suspend fun writePin(
        serverUrl: String,
        authToken: String,
        virtualPin: String,
        targetState: LightPowerState,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (targetState == LightPowerState.UNKNOWN) {
            return@withContext Result.failure(BlynkApiException("Cannot write UNKNOWN state"))
        }
        val base = normalizeServerUrl(serverUrl)
            ?: return@withContext Result.failure(BlynkApiException("Invalid Blynk server URL"))
        val token = authToken.trim()
        if (token.isEmpty()) {
            return@withContext Result.failure(BlynkApiException("Missing Blynk AuthToken"))
        }
        val pin = normalizePinForApi(virtualPin)
        val value = if (targetState == LightPowerState.ON) "1" else "0"
        val url = "$base/external/api/update".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("token", token)
            ?.addQueryParameter(pin, value)
            ?.build()
            ?: return@withContext Result.failure(BlynkApiException("Malformed Blynk URL"))

        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        BlynkApiException(
                            message = parseBlynkError(body, response.code),
                            isConnectivityFailure = response.code >= 500 || response.code == 429,
                            httpCode = response.code,
                        )
                    )
                }
                Result.success(Unit)
            }
        } catch (e: IOException) {
            Result.failure(
                BlynkApiException(
                    message = e.message ?: "Network unreachable",
                    isConnectivityFailure = true,
                )
            )
        }
    }

    /**
     * Strict 3-step toggle mechanism:
     * 1. Read current Virtual Pin value from Blynk Cloud.
     * 2. Determine opposite state (abort if unreadable; never assume OFF).
     * 3. Send new state to Blynk Cloud.
     */
    override suspend fun toggleLight(
        serverUrl: String,
        authToken: String,
        virtualPin: String,
    ): Result<LightPowerState> {
        val currentResult = readPin(serverUrl, authToken, virtualPin)
        val currentState = currentResult.getOrElse { return Result.failure(it) }
        val nextState = when (currentState) {
            LightPowerState.ON -> LightPowerState.OFF
            LightPowerState.OFF -> LightPowerState.ON
            LightPowerState.UNKNOWN -> {
                return Result.failure(BlynkApiException("Unreadable light state; toggle aborted"))
            }
        }
        return writePin(serverUrl, authToken, virtualPin, nextState).map { nextState }
    }

    override suspend fun testConnectivity(
        serverUrl: String,
        sampleToken: String?,
    ): ConnectivityTestResult = withContext(Dispatchers.IO) {
        val base = normalizeServerUrl(serverUrl)
            ?: return@withContext ConnectivityTestResult(
                serverReachable = false,
                hardwareConnected = null,
                message = "Invalid Blynk server URL format.",
            )

        val tokenToTest = sampleToken?.trim()?.takeIf { it.isNotEmpty() }
        val queryToken = tokenToTest ?: "connectivity_check"
        val url = "$base/external/api/isHardwareConnected".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("token", queryToken)
            ?.build()
            ?: return@withContext ConnectivityTestResult(
                serverReachable = false,
                hardwareConnected = null,
                message = "Could not build connectivity check URL.",
            )

        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty().trim()
                if (response.isSuccessful && tokenToTest != null) {
                    val connected = body.equals("true", ignoreCase = true)
                    return@withContext ConnectivityTestResult(
                        serverReachable = true,
                        hardwareConnected = connected,
                        message = if (connected) {
                            "Blynk server reachable • ESP32 hardware is online."
                        } else {
                            "Blynk server reachable • ESP32 hardware currently offline."
                        },
                    )
                }
                if (response.code in 400..499) {
                    // Server responded over HTTPS/HTTP cleanly; invalid dummy token proves endpoint works.
                    val detail = if (tokenToTest == null) {
                        "Blynk server reachable (add a light token to verify hardware status)."
                    } else {
                        "Blynk server reachable, but token check returned: ${parseBlynkError(body, response.code)}"
                    }
                    return@withContext ConnectivityTestResult(
                        serverReachable = true,
                        hardwareConnected = null,
                        message = detail,
                    )
                }
                ConnectivityTestResult(
                    serverReachable = false,
                    hardwareConnected = null,
                    message = "Server returned HTTP ${response.code}.",
                )
            }
        } catch (e: IOException) {
            ConnectivityTestResult(
                serverReachable = false,
                hardwareConnected = null,
                message = "Cannot reach server: ${e.message ?: "Network error"}",
            )
        }
    }

    companion object {
        fun createDefaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun normalizeServerUrl(raw: String): String? {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
            val parsed = withScheme.toHttpUrlOrNull() ?: return null
            return parsed.toString().trimEnd('/')
        }

        fun normalizePinForApi(raw: String): String {
            val trimmed = raw.trim().lowercase(Locale.ROOT)
            if (trimmed.isEmpty()) return "v0"
            return if (trimmed.startsWith("v")) trimmed else "v$trimmed"
        }

        fun formatPinForDisplay(raw: String): String {
            val normalized = normalizePinForApi(raw)
            return normalized.uppercase(Locale.ROOT)
        }

        fun parseSinglePinResponse(rawBody: String): LightPowerState? {
            val trimmed = rawBody.trim()
            if (trimmed.isEmpty()) return null
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val inner = trimmed.substring(1, trimmed.length - 1).trim()
                if (inner.isEmpty()) return null
                val firstElement = inner.split(",").firstOrNull()?.trim() ?: return null
                return parseScalarValue(firstElement)
            }
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val map = parseFlatJsonObject(trimmed) ?: return null
                if (map.containsKey("error")) return null
                val firstVal = map.values.firstOrNull() ?: return null
                return parseScalarValue(firstVal)
            }
            return parseScalarValue(trimmed)
        }

        fun parseMultiPinResponse(
            rawBody: String,
            expectedNormalizedPins: List<String>,
        ): Map<String, LightPowerState> {
            val trimmed = rawBody.trim()
            if (trimmed.isEmpty()) return emptyMap()
            if (!trimmed.startsWith("{")) {
                if (expectedNormalizedPins.size == 1) {
                    val parsed = parseSinglePinResponse(trimmed) ?: return emptyMap()
                    return mapOf(expectedNormalizedPins.first() to parsed)
                }
                return emptyMap()
            }
            val lowerMap = parseFlatJsonObject(trimmed) ?: return emptyMap()
            if (lowerMap.containsKey("error")) return emptyMap()
            val result = mutableMapOf<String, LightPowerState>()
            for (pin in expectedNormalizedPins) {
                val rawVal = lowerMap[pin.lowercase(Locale.ROOT)] ?: continue
                val state = parseScalarValue(rawVal) ?: continue
                result[pin] = state
            }
            return result
        }

        private fun parseFlatJsonObject(json: String): Map<String, String>? {
            val trimmed = json.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
            val body = trimmed.substring(1, trimmed.length - 1).trim()
            if (body.isEmpty()) return emptyMap()

            // Detect top-level error object quickly
            val errorRegex = Regex("\"error\"\\s*:\\s*\\{")
            if (errorRegex.containsMatchIn(body)) {
                return mapOf("error" to "true")
            }

            val pairRegex = Regex("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|[^,}\\s]+)")
            val map = mutableMapOf<String, String>()
            for (match in pairRegex.findAll(body)) {
                val key = match.groupValues[1].lowercase(Locale.ROOT)
                val value = match.groupValues[2].trim().trim('"')
                map[key] = value
            }
            return map
        }

        private fun parseScalarValue(raw: String): LightPowerState? {
            val cleaned = raw.trim().trim('"')
            if (cleaned.isEmpty() || cleaned.equals("null", ignoreCase = true)) return null
            if (cleaned.equals("true", ignoreCase = true)) return LightPowerState.ON
            if (cleaned.equals("false", ignoreCase = true)) return LightPowerState.OFF
            val number = cleaned.toDoubleOrNull() ?: return null
            if (!number.isFinite()) return null
            return if (number == 0.0) LightPowerState.OFF else LightPowerState.ON
        }

        private fun parseBlynkError(body: String, code: Int): String {
            val msgRegex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"")
            val msg = msgRegex.find(body)?.groupValues?.getOrNull(1)
            return if (!msg.isNullOrBlank()) msg else "HTTP $code"
        }
    }
}
