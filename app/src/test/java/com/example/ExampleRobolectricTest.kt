package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.BlynkController
import com.example.data.SettingsRepository
import com.example.model.AppSettings
import com.example.model.CloudStatus
import com.example.model.LightCardConfig
import com.example.model.LightPowerState
import com.example.model.LightRuntimeState
import com.example.network.BlynkApi
import com.example.network.BlynkApiException
import com.example.network.ConnectivityTestResult
import com.example.ui.DashboardUiState
import com.example.ui.home.HomeScreen
import com.example.ui.theme.BlynkLightTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name and launcher resources are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Blynk Light", context.getString(R.string.app_name))
    }

    @Test
    fun `clock button does not trigger card toggle and opens deferred timer dialog`() {
        val card = LightCardConfig(
            id = "light-1",
            name = "Desk Lamp",
            authToken = "token123456",
            virtualPin = "V0",
        )
        var toggleCount = 0
        var settingsOpened = false

        composeRule.setContent {
            BlynkLightTheme {
                HomeScreen(
                    uiState = DashboardUiState(
                        lights = listOf(card),
                        runtimeStates = mapOf(
                            card.id to LightRuntimeState(powerState = LightPowerState.ON),
                        ),
                        settings = AppSettings(),
                        cloudStatus = CloudStatus.ONLINE,
                    ),
                    onToggleLight = { toggleCount++ },
                    onOpenSettings = { settingsOpened = true },
                    onRefreshNow = {},
                    onDismissError = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("ONLINE").assertIsDisplayed()
        composeRule.onNodeWithText("Desk Lamp").assertIsDisplayed()

        // Tap clock icon: must NOT toggle the light
        composeRule.onNodeWithTag("timer_button_light-1").performClick()
        composeRule.waitForIdle()
        assertEquals(0, toggleCount)
        composeRule.onNodeWithText("Desk Lamp Timer").assertIsDisplayed()
        composeRule.onNodeWithText("Got it").performClick()
        composeRule.waitForIdle()

        // Tap light card itself: MUST toggle the light
        composeRule.onNodeWithTag("light_card_light-1").performClick()
        composeRule.waitForIdle()
        assertEquals(1, toggleCount)

        // Tap settings icon
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.waitForIdle()
        assertTrue(settingsOpened)
    }

    @Test
    fun `controller preserves previous ON state and sets OFFLINE when toggle fails`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)
        val saved = repo.addLight("Hallway", "valid_token_abc", "V2")

        var shouldFail = false
        val fakeApi = object : BlynkApi {
            override suspend fun readPin(serverUrl: String, authToken: String, virtualPin: String) =
                if (shouldFail) {
                    Result.failure(BlynkApiException("Timeout", isConnectivityFailure = true))
                } else {
                    Result.success(LightPowerState.OFF)
                }

            override suspend fun readMultiplePins(
                serverUrl: String,
                authToken: String,
                virtualPins: List<String>,
            ) = Result.success(mapOf("v2" to LightPowerState.OFF))

            override suspend fun writePin(
                serverUrl: String,
                authToken: String,
                virtualPin: String,
                targetState: LightPowerState,
            ) = Result.success(Unit)

            override suspend fun toggleLight(
                serverUrl: String,
                authToken: String,
                virtualPin: String,
            ): Result<LightPowerState> {
                return if (shouldFail) {
                    Result.failure(BlynkApiException("Connection lost", isConnectivityFailure = true))
                } else {
                    Result.success(LightPowerState.ON)
                }
            }

            override suspend fun testConnectivity(serverUrl: String, sampleToken: String?) =
                ConnectivityTestResult(!shouldFail, null, "ok")
        }

        val controller = BlynkController(context, repo, fakeApi)

        // First toggle succeeds -> state becomes ON, cloud ONLINE
        val first = controller.toggleLight(saved.id)
        assertTrue(first.isSuccess)
        assertEquals(LightPowerState.ON, controller.lightStates.value[saved.id]?.powerState)
        assertEquals(CloudStatus.ONLINE, controller.cloudStatus.value)

        // Second toggle fails -> state MUST stay ON (not reverted!) and cloud becomes OFFLINE
        shouldFail = true
        val second = controller.toggleLight(saved.id)
        assertFalse(second.isSuccess)
        assertEquals(LightPowerState.ON, controller.lightStates.value[saved.id]?.powerState)
        assertEquals(CloudStatus.OFFLINE, controller.cloudStatus.value)

        // Clean up
        repo.deleteLight(saved.id)
    }
}
