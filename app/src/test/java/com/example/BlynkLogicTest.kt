package com.example

import com.example.model.LightPowerState
import com.example.network.BlynkApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlynkLogicTest {

    @Test
    fun `parseSinglePinResponse handles Blynk scalar and array values safely`() {
        assertEquals(LightPowerState.OFF, BlynkApiClient.parseSinglePinResponse("0"))
        assertEquals(LightPowerState.ON, BlynkApiClient.parseSinglePinResponse("1"))
        assertEquals(LightPowerState.ON, BlynkApiClient.parseSinglePinResponse("[\"1\"]"))
        assertEquals(LightPowerState.OFF, BlynkApiClient.parseSinglePinResponse("[\"0\"]"))
        assertEquals(LightPowerState.ON, BlynkApiClient.parseSinglePinResponse("true"))
        assertEquals(LightPowerState.OFF, BlynkApiClient.parseSinglePinResponse("false"))

        // Unreadable or error payloads MUST return null (never assume OFF!)
        assertNull(BlynkApiClient.parseSinglePinResponse(""))
        assertNull(BlynkApiClient.parseSinglePinResponse("null"))
        assertNull(BlynkApiClient.parseSinglePinResponse("{\"error\":{\"message\":\"Invalid token\"}}"))
        assertNull(BlynkApiClient.parseSinglePinResponse("not_a_number"))
    }

    @Test
    fun `parseMultiPinResponse parses batch JSON case-insensitively without assuming missing pins are OFF`() {
        val parsed = BlynkApiClient.parseMultiPinResponse(
            rawBody = "{\"V0\":\"1\",\"v1\":0,\"v2\":\"invalid\"}",
            expectedNormalizedPins = listOf("v0", "v1", "v2", "v3"),
        )
        assertEquals(LightPowerState.ON, parsed["v0"])
        assertEquals(LightPowerState.OFF, parsed["v1"])
        assertNull(parsed["v2"])
        assertNull(parsed["v3"])
    }

    @Test
    fun `normalizeServerUrl and pin formatting handle user input variants`() {
        assertEquals("https://blynk.cloud", BlynkApiClient.normalizeServerUrl("blynk.cloud"))
        assertEquals("https://fra1.blynk.cloud", BlynkApiClient.normalizeServerUrl("https://fra1.blynk.cloud/"))
        assertEquals("http://192.168.1.50:8080", BlynkApiClient.normalizeServerUrl("http://192.168.1.50:8080/"))
        assertNull(BlynkApiClient.normalizeServerUrl("   "))

        assertEquals("v0", BlynkApiClient.normalizePinForApi("V0"))
        assertEquals("v3", BlynkApiClient.normalizePinForApi("3"))
        assertEquals("V12", BlynkApiClient.formatPinForDisplay("v12"))
    }
}
