package dev.maruffirdaus.trial.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

object ApkUtil {
    suspend fun install(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val path = getPathFromUri(uri)
                ?: (throw FileNotFoundException("File path could not be resolved from URI: $uri"))
            val output = Runtime.getRuntime().exec("su -c pm install -r -g $path").let { process ->
                process.inputStream.bufferedReader().use { it.readText() }
            }
            if (output.contains("Success") || output.isEmpty()) {
                val packageInfo = File(path).let {
                    context.packageManager.getPackageArchiveInfo(it.absolutePath, 0)
                }
                return@withContext packageInfo?.packageName
            } else {
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun launch(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Runtime.getRuntime()
                .exec("su -c monkey -p $packageName -c android.intent.category.LAUNCHER 1")
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun kill(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Runtime.getRuntime().exec("su -c am force-stop $packageName")
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun uninstall(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val output =
                Runtime.getRuntime().exec("su -c pm uninstall $packageName").let { process ->
                    process.inputStream.bufferedReader().use { it.readText() }
                }
            return@withContext output.contains("Success") || output.isEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        val parts = docId.split(":")
        if (parts.size == 2 && parts[0] == "primary") {
            return copyFileToTemp("/storage/emulated/0/${parts[1]}")
        }
        return null
    }

    private fun copyFileToTemp(path: String): String? {
        val tempPath = "/data/local/tmp/${File(path).name}"
        try {
            Runtime.getRuntime().exec("su -c cp \"$path\" \"$tempPath\" && chmod 777 \"$tempPath\"")
                .waitFor()
            return tempPath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}