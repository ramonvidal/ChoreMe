package com.choreme.core.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Whether Android's battery saver may put this app to sleep, which stops the family location in the background. Asking
 * to be left out directly is not allowed on Google Play, so the person is sent to the settings page to do it by hand.
 */
object BatteryOptimization {
    /** True when the app is under the battery saver's limits (the default for every app). */
    fun isRestricted(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return !power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** The list of apps with their battery setting; the app's own settings page when the phone has no such list. */
    fun settingsIntent(context: Context): Intent {
        val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (list.resolveActivity(context.packageManager) != null) return list
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
}
