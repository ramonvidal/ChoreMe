package com.choreme.data

import com.choreme.domain.session.Account
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.Lazy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parents sign in with Google. Kids may not have a Google account, so their devices
 * sign in anonymously and get into the family only once a parent approves them.
 */
@Singleton
class AuthRepository @Inject constructor(
    // Lazy: FirebaseAuth throws if the app was built without Firebase config.
    private val auth: Lazy<FirebaseAuth>,
) {
    val account: Flow<Account?> = callbackFlow {
        val firebaseAuth = auth.get()
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAccount()) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun currentAccount(): Account? = auth.get().currentUser?.toAccount()

    suspend fun signInWithGoogle(idToken: String) {
        auth.get().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun signInAsKid() {
        auth.get().signInAnonymously().await()
    }

    fun signOut() = auth.get().signOut()

    private fun FirebaseUser.toAccount() = Account(uid = uid, isAnonymous = isAnonymous, displayName = displayName)
}
