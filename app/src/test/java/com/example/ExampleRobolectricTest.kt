package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BlynkPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Blynk Light", appName)
    }

    @Test
    fun `check default template and pin configurations`() {
        assertEquals("TMPL6mWdFodq6", BlynkPreferences.DEFAULT_TEMPLATE_ID)
        assertEquals("Đèn Thông Minh", BlynkPreferences.DEFAULT_TEMPLATE_NAME)
        assertEquals("v0", BlynkPreferences.DEFAULT_PIN_1)
        assertEquals("v1", BlynkPreferences.DEFAULT_PIN_2)
    }
}
