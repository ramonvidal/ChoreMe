package com.choreme.feature.onboarding

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Shows the Google account picker and returns an ID token for Firebase.
 * [activityContext] must be an Activity: Credential Manager draws its UI over it.
 */
@SuppressLint("DiscouragedApi") // The resource only exists when built with google-services.json.
suspend fun requestGoogleIdToken(activityContext: Context): String {
    val clientIdRes = activityContext.resources.getIdentifier("default_web_client_id", "string", activityContext.packageName)
    check(clientIdRes != 0) { "google-services.json has no web client ID; enable Google sign-in in Firebase" }

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(activityContext.getString(clientIdRes)).build())
        .build()
    val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Unexpected credential type ${credential.type}"
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
