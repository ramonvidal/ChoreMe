package com.choreme.core.data

import android.content.Context
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase initializes itself only when the app was built with google-services.json. */
@Singleton
class FirebaseAvailability @Inject constructor(@ApplicationContext context: Context) {
    val isConfigured: Boolean = FirebaseApp.getApps(context).isNotEmpty()
}
