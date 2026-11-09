package com.sms2api.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sms2api.app.ui.screens.MainScreen
import com.sms2api.app.ui.theme.SMS2APITheme
import com.sms2api.app.ui.viewmodel.MainViewModel
import com.sms2api.app.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.checkBatteryOptimization(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val targetLocale = remember(uiState.appLanguage) {
                LocaleHelper.getLocale(uiState.appLanguage)
            }
            val configuration = remember(targetLocale) {
                Configuration(resources.configuration).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
            }
            val localizedContext = remember(targetLocale, configuration) {
                createConfigurationContext(configuration)
            }
            val layoutDirection = if (configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalLayoutDirection provides layoutDirection
            ) {
                SMS2APITheme {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkBatteryOptimization(this)
        viewModel.refreshServiceStatus()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
