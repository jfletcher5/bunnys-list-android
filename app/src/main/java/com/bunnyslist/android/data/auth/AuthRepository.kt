package com.bunnyslist.android.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.bunnyslist.android.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** User dismissed the Google account picker. Not a failure to show in the UI. */
class SignInCancelledException : Exception("Sign-in cancelled")

/**
 * Google sign-in through Credential Manager, then Firebase Auth.
 * The server client id is [R.string.default_web_client_id], which the Google
 * Services plugin generates from the client_type 3 OAuth client in
 * google-services.json. Do not hardcode that id.
 */
@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
) {
    private val credentialManager = CredentialManager.create(context)

    private val webClientId: String
        get() = context.getString(R.string.default_web_client_id)

    fun observeUser(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(activity: Activity): FirebaseUser {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        val result = try {
            credentialManager.getCredential(context = activity, request = request)
        } catch (cancelled: GetCredentialCancellationException) {
            throw SignInCancelledException()
        }

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Google sign-in did not return an ID token.")
        }

        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
        val authResult = auth.signInWithCredential(firebaseCredential).await()
        return authResult.user ?: error("Firebase Auth did not return a user.")
    }

    suspend fun signOut() {
        auth.signOut()
        // Clear the Credential Manager session so the next sign-in asks again.
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
