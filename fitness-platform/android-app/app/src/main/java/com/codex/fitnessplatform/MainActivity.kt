package com.codex.fitnessplatform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.fitnessplatform.ui.FitnessPlatformScreen
import com.codex.fitnessplatform.ui.FitnessPlatformViewModel
import com.codex.fitnessplatform.ui.theme.FitnessPlatformTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as FitnessPlatformApp
            val viewModel: FitnessPlatformViewModel = viewModel(
                factory = FitnessPlatformViewModel.Factory(app.repository)
            )

            FitnessPlatformTheme {
                FitnessPlatformScreen(viewModel = viewModel)
            }
        }
    }
}
