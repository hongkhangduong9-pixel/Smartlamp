package com.example

import android.app.Application
import com.example.data.BlynkController
import com.example.data.SettingsRepository
import com.example.network.BlynkApiClient

class BlynkLightApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var blynkController: BlynkController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        blynkController = BlynkController(
            context = this,
            repository = settingsRepository,
            api = BlynkApiClient(),
        )
    }
}
