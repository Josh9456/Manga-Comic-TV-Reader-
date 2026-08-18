package com.mangatv.reader.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String?,
    val apkFileName: String?
)

object AppUpdateManager {

    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/Josh9456/Manga-Comic-TV-Reader-/releases"

    fun getCurrentVersion(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.6"
        } catch (e: Exception) {
            "1.0.6"
        }
    }

    suspend fun checkForUpdates(context: Context): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion(context)
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MangaTV-AppUpdateManager")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("GitHub API returned status ${connection.responseCode}"))
            }

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val releasesArray = org.json.JSONArray(jsonString)

            if (releasesArray.length() == 0) {
                return@withContext Result.failure(Exception("No releases found on GitHub repository"))
            }

            var targetRelease: JSONObject? = null
            var downloadUrl: String? = null
            var apkFileName: String? = null

            // Find the most recent release that has an APK uploaded
            for (i in 0 until releasesArray.length()) {
                val release = releasesArray.getJSONObject(i)
                val assets = release.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            targetRelease = release
                            downloadUrl = asset.optString("browser_download_url")
                            apkFileName = name
                            break
                        }
                    }
                }
                if (targetRelease != null) break
            }

            if (targetRelease == null) {
                targetRelease = releasesArray.getJSONObject(0)
            }

            val tagName = targetRelease.optString("tag_name", "").trim()
            val releaseTitle = targetRelease.optString("name", tagName)
            val releaseNotes = targetRelease.optString("body", "")

            val cleanLatest = tagName.removePrefix("v").trim()
            val cleanCurrent = currentVersion.removePrefix("v").trim()

            val isAvailable = cleanLatest.isNotEmpty() && cleanLatest != cleanCurrent && downloadUrl != null

            Result.success(
                AppUpdateInfo(
                    isUpdateAvailable = isAvailable,
                    currentVersion = currentVersion,
                    latestVersion = cleanLatest.ifEmpty { tagName },
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    apkFileName = apkFileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outputFile = File(updatesDir, fileName.ifEmpty { "MangaTV-update.apk" })

            if (outputFile.exists()) {
                outputFile.delete()
            }

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MangaTV-AppUpdateManager")
                connectTimeout = 15000
                readTimeout = 30000
            }

            val totalBytes = connection.contentLength
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            onProgress(progress.coerceIn(0, 100))
                        }
                    }
                }
            }

            // Launch package installer
            withContext(Dispatchers.Main) {
                installApk(context, outputFile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
