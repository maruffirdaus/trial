package app.apktracer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.apktracer.ui.home.HomeScreen
import app.apktracer.ui.home.HomeViewModel
import app.apktracer.ui.theme.ApkTracerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApkTracerTheme {
                val viewModel: HomeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                HomeScreen(
                    uiState = uiState,
                    traceApk = viewModel::traceApk,
                    refreshLogs = viewModel::refreshLogs
                )
            }
        }
    }
}