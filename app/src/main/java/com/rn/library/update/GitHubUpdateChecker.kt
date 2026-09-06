package com.rn.library.update

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tag: String,
    val name: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkName: String?,
)

sealed class UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Available(val currentVersion: String, val release: GitHubRelease) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object GitHubUpdateChecker {
    const val OWNER = "RealNeal"
    const val REPO = "MyLib"
    private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    private const val PREFS = "app_prefs"
    private const val KEY_LAST_BACKGROUND_CHECK = "last_background_update_check"
    private const val BACKGROUND_CHECK_INTERVAL_MS = 3L * 24 * 60 * 60 * 1000

    fun currentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (_: PackageManager.NameNotFoundException) {
            "0"
        }
    }

    fun check(context: Context): UpdateCheckResult {
        val current = currentVersionName(context)
        return try {
            val release = fetchLatestRelease()
                ?: return UpdateCheckResult.Error("empty")
            if (isNewer(release.tag, current)) {
                UpdateCheckResult.Available(current, release)
            } else {
                UpdateCheckResult.UpToDate(current)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "error")
        }
    }

    /** Background checks are limited to one request every three days. */
    fun shouldRunBackgroundCheck(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val lastCheck = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKGROUND_CHECK, 0L)
        return now - lastCheck >= BACKGROUND_CHECK_INTERVAL_MS
    }

    fun markBackgroundCheck(context: Context, now: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKGROUND_CHECK, now)
            .apply()
    }

    fun isNewer(latestTag: String, currentVersion: String): Boolean =
        compareVersions(latestTag, currentVersion) > 0

    internal fun compareVersions(left: String, right: String): Int {
        val a = versionParts(left)
        val b = versionParts(right)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun versionParts(raw: String): List<Int> =
        raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '_')
            .mapNotNull { part -> part.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toInt() }

    private fun fetchLatestRelease(): GitHubRelease? {
        val connection = open(LATEST_URL)
        try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank { return null }
            val assets = json.optJSONArray("assets") ?: return GitHubRelease(
                tag = tag,
                name = json.optString("name").ifBlank { tag },
                htmlUrl = json.optString("html_url"),
                apkUrl = null,
                apkName = null,
            )
            var apkUrl: String? = null
            var apkName: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").ifBlank { null }
                    apkName = name
                    break
                }
            }
            return GitHubRelease(
                tag = tag,
                name = json.optString("name").ifBlank { tag },
                htmlUrl = json.optString("html_url"),
                apkUrl = apkUrl,
                apkName = apkName,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "MyLib-Android")
        }
        return connection
    }
}
