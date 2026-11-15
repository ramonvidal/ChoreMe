package com.choreme

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.choreme.feature.chores.ChoreReminders
import com.choreme.feature.home.WeatherAlerts
import com.choreme.feature.map.PlaceAlerts
import com.choreme.core.settings.DiagnosticsSettings
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChoreMeApp : Application(), Configuration.Provider {
    // Lets background workers (chore reminders) get their dependencies from Hilt.
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var diagnostics: DiagnosticsSettings

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ChoreReminders.schedule(this)
        WeatherAlerts.schedule(this)
        PlaceAlerts.schedule(this)
        applyCrashReportChoice()
    }

    /** Crash reports follow the switch in Settings; nothing is sent until this runs, and never without Firebase set up. */
    private fun applyCrashReportChoice() {
        if (FirebaseApp.getApps(this).isEmpty()) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            diagnostics.crashReports.collect { on -> FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(on) }
        }
    }
}
