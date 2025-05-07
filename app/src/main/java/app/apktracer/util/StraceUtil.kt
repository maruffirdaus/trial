package app.apktracer.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StraceUtil {
    suspend fun tracePackage(context: Context, packageName: String, timeout: Int = 30): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!isStraceAvailable(context)) extractStraceBinary(context)

                val pid = findProcessId(packageName)
                    ?: throw IllegalStateException("Could not find process ID for package: $packageName")

                val timestamp =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                val outputFile = File(
                    context.getExternalFilesDir(null),
                    "Logs/${timestamp}_${packageName}.txt"
                )
                outputFile.parentFile?.mkdirs()

                val process = ProcessBuilder(
                    "su",
                    "-c",
                    "timeout $timeout ${stracePath(context)} -f -tt -p $pid"
                ).redirectErrorStream(true).start()

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

                val exitCode = process.waitFor()

                return@withContext exitCode == 0 || exitCode == 124
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }

    private suspend fun findProcessId(packageName: String): Int? = withContext(Dispatchers.IO) {
        try {
            var process = ProcessBuilder("su", "-c", "ps -A | grep $packageName").start()
            process.waitFor()

            var output = process.inputStream.bufferedReader().use { it.readText() }

            if (output.isBlank()) {
                process = ProcessBuilder("su", "-c", "ps | grep $packageName").start()
                process.waitFor()

                output = process.inputStream.bufferedReader().use { it.readText() }
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

    private fun extractStraceBinary(context: Context) {
        val straceFile = File(stracePath(context))

        try {
            context.assets.open("strace-x64").use { input ->
                FileOutputStream(straceFile).use { output ->
                    input.copyTo(output)
                }
            }
            straceFile.setExecutable(true, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stracePath(context: Context): String = "${context.filesDir.absolutePath}/strace"

    private fun isStraceAvailable(context: Context): Boolean = File(stracePath(context)).let {
        return@let it.exists() && it.canExecute()
    }
}