package com.choreme

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.choreme.feature.chores.ChoreReminders
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChoreMeApp : Application(), Configuration.Provider {
    // Lets background workers (chore reminders) get their dependencies from Hilt.
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ChoreReminders.schedule(this)
    }
}
