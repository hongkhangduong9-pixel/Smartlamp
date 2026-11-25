package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.BlynkLightTheme

const val ROUTE_HOME = "home"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val app = application as BlynkLightApplication

        setContent {
            BlynkLightTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val connectivityResult by viewModel.connectivityTestResult.collectAsStateWithLifecycle()
                val isTestingConnectivity by viewModel.isTestingConnectivity.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> app.blynkController.startForegroundSync()
                            Lifecycle.Event.ON_STOP -> app.blynkController.stopForegroundSync()
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        app.blynkController.stopForegroundSync()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    enterTransition = { fadeIn() + slideInHorizontally { it / 6 } },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() + slideOutHorizontally { it / 6 } },
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            uiState = uiState,
                            onToggleLight = viewModel::onToggleLight,
                            onOpenSettings = {
                                viewModel.clearConnectivityTestResult()
                                navController.navigate(ROUTE_SETTINGS) {
                                    launchSingleTop = true
                                }
                            },
                            onRefreshNow = viewModel::onRefreshNow,
                            onDismissError = viewModel::dismissErrorBanner,
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            connectivityTestResult = connectivityResult,
                            isTestingConnectivity = isTestingConnectivity,
                            onBack = { navController.popBackStack() },
                            onAddLight = viewModel::addLight,
                            onUpdateLight = viewModel::updateLight,
                            onDeleteLight = viewModel::deleteLight,
                            onUpdateServerUrl = viewModel::updateServerUrl,
                            onTestConnectivity = viewModel::testServerConnectivity,
                            onUpdateSyncIntervalMs = viewModel::updateSyncIntervalMs,
                            onUpdateTileAssignment = viewModel::updateTileAssignment,
                            onSelectBackgroundUri = viewModel::selectBackgroundImage,
                            onUpdateTheme = viewModel::updateTheme,
                            onClearBackground = viewModel::clearBackgroundImage,
                        )
                    }
                }
            }
        }
    }
}
