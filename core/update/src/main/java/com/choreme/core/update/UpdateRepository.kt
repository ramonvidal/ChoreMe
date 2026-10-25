package com.choreme.core.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.choreme.core.model.AppUpdate
import com.choreme.core.model.Updates
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Asks the release page for the newest build. Free and anonymous: no account or key involved. */
@Singleton
class UpdateRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    val installedBuild: Long
        get() {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        }

    /** The newest build if it's newer than this one and not put off with "Later"; checks at most daily. */
    suspend fun availableUpdate(force: Boolean = false): AppUpdate? = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val cached = cachedUpdate()
        val latest = if (force || Updates.isCheckDue(lastCheck(), now)) {
            runCatching { fetchLatest() }.getOrNull()?.also { remember(it, now) } ?: cached
        } else {
            cached
        }
        latest?.takeIf { Updates.shouldOffer(it, installedBuild, dismissedBuild()) }
    }

    fun dismiss(update: AppUpdate) {
        prefs.edit().putInt(KEY_DISMISSED, update.buildNumber).apply()
    }

    private fun fetchLatest(): AppUpdate? {
        val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val build = Updates.buildNumber(json.optString("tag_name")) ?: return null
            val assets = json.optJSONArray("assets") ?: return null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name") == APK_NAME }
                ?: return null
            return AppUpdate(build, "0.$build", apk.getString("browser_download_url"), apk.optLong("size"))
        } finally {
            connection.disconnect()
        }
    }

    private fun lastCheck(): Instant? = prefs.getLong(KEY_CHECKED, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    private fun dismissedBuild(): Int? = prefs.getInt(KEY_DISMISSED, -1).takeIf { it >= 0 }

    private fun cachedUpdate(): AppUpdate? {
        val build = prefs.getInt(KEY_BUILD, -1).takeIf { it >= 0 } ?: return null
        val url = prefs.getString(KEY_URL, null) ?: return null
        return AppUpdate(build, "0.$build", url, prefs.getLong(KEY_SIZE, 0L))
    }

    private fun remember(update: AppUpdate?, now: Instant) {
        val edit = prefs.edit().putLong(KEY_CHECKED, now.toEpochMilli())
        if (update != null) {
            edit.putInt(KEY_BUILD, update.buildNumber).putString(KEY_URL, update.downloadUrl).putLong(KEY_SIZE, update.sizeBytes)
        }
        edit.apply()
    }

    companion object {
        /** Where the builds are published (see .github/workflows/android.yml). */
        const val LATEST_RELEASE_API = "https://api.github.com/repos/ramonvidal/ChoreMe/releases/latest"
        const val APK_NAME = "ChoreMe.apk"
        private const val TIMEOUT_MS = 15_000
        private const val KEY_CHECKED = "checked_at"
        private const val KEY_DISMISSED = "dismissed_build"
        private const val KEY_BUILD = "latest_build"
        private const val KEY_URL = "latest_url"
        private const val KEY_SIZE = "latest_size"
    }
}
