package app.apktracer.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import app.apktracer.util.ApkUtil
import app.apktracer.util.StraceUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    @OptIn(DelicateCoroutinesApi::class)
    fun traceApk(context: Context, apkUri: Uri, onSuccess: (Boolean) -> Unit) {
        GlobalScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            ApkUtil.install(context, apkUri).let { packageName ->
                if (packageName != null) {
                    ApkUtil.launch(packageName)
                    delay(1000)

                    val result = StraceUtil.tracePackage(context, packageName, 30)

                    ApkUtil.kill(packageName)
                    ApkUtil.uninstall(packageName)

                    refreshLogs(context)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(result)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(false)
                }
            }
        }
    }

    fun refreshLogs(context: Context) {
        val filesDir = context.getExternalFilesDir("Logs")
        _uiState.update {
            it.copy(
                logs = filesDir?.listFiles()?.map { file -> file.name }?.reversed() ?: emptyList()
            )
        }
    }
}