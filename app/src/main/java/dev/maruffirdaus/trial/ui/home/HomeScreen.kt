package dev.maruffirdaus.trial.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.maruffirdaus.trial.ui.theme.TrialTheme
import dev.maruffirdaus.trial.util.ApkUtil
import dev.maruffirdaus.trial.util.StraceUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }

    val getApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            MainScope().launch {
                isLoading = true
                ApkUtil.install(context, uri)?.let { packageName ->
                    ApkUtil.launch(packageName)
                    StraceUtil.tracePackage(context, packageName)
                    ApkUtil.kill(packageName)
                    ApkUtil.uninstall(packageName)
                }
                isLoading = false
            }
        }
    }

    Scaffold { innerPadding ->
        if (isLoading)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        else
            Column(
                Modifier.padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            getApkLauncher.launch("application/vnd.android.package-archive")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 64.dp),
                        enabled = !isLoading
                    ) {
                        Text("Select APK")
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Logs",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + innerPadding.calculateBottomPadding()
                    )
                ) {
                }
            }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    TrialTheme {
        HomeScreen()
    }
}