package dev.maruffirdaus.trial.util

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StraceUtil {
    @SuppressLint("SdCardPath")
    private const val TERMUX_STRACE_PATH = "/data/data/com.termux/files/usr/bin/strace"

    suspend fun tracePackage(context: Context, packageName: String, timeout: Int = 5): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!isStraceAvailable()) throw FileNotFoundException("Termux strace binary not found at $TERMUX_STRACE_PATH")

                val pid = findProcessId(packageName)
                    ?: throw IllegalStateException("Could not find process ID for package: $packageName")

                val timestamp =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                val outputFile = File(
                    context.getExternalFilesDir(null),
                    "Strace Logs/${packageName}_${timestamp}.log"
                )
                outputFile.parentFile?.mkdirs()

                val process = Runtime.getRuntime().exec("timeout $timeout su -c $TERMUX_STRACE_PATH -f -tt -p $pid")

                FileOutputStream(outputFile).use { fileOutput ->
                    process.inputStream.bufferedReader().use { reader ->
                        val buffer = CharArray(4096)
                        var read: Int

                        while (reader.read(buffer).also { read = it } != -1) {
                            val outputString = String(buffer, 0, read)
                            fileOutput.write(outputString.toByteArray())
                        }
                    }
                }

                return@withContext process.waitFor() == 0
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }

    private suspend fun findProcessId(packageName: String): Int? = withContext(Dispatchers.IO) {
        try {
            val output =
                Runtime.getRuntime().exec("su -c ps -A | grep $packageName").let { process ->
                    process.inputStream.bufferedReader().use { it.readText() }
                }.ifBlank {
                    Runtime.getRuntime().exec("su -c ps | grep $packageName").let { process ->
                        process.inputStream.bufferedReader().use { it.readText() }
                    }
                }
            val pid = output.split("\n")
                .filter { it.contains(packageName) }
                .firstNotNullOfOrNull { line ->
                    line.trim().split("\\s+".toRegex()).let { parts ->
                        if (1 < parts.size)
                            parts[1].toIntOrNull().takeIf { 0 < (it ?: -1) }
                        else null
                    }
                }
            return@withContext pid
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun isStraceAvailable(): Boolean = File(TERMUX_STRACE_PATH).let {
        return@let it.exists() && it.canExecute()
    }
}