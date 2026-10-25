package com.choreme.core.push

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Sends a wake-up ping to other phones. Best effort: a phone that misses one catches up on its next check. */
@Singleton
class PushSender @Inject constructor() {
    suspend fun ping(endpoints: Collection<String>, kind: String) = withContext(Dispatchers.IO) {
        endpoints.forEach { url ->
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/plain")
                // Ask the push server not to keep it long: a stale ping is useless.
                connection.setRequestProperty("TTL", "3600")
                connection.outputStream.use { it.write(kind.toByteArray()) }
                connection.responseCode
                connection.disconnect()
            } catch (e: Exception) {
                Log.w("ChoreMe", "Ping failed", e)
            }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
