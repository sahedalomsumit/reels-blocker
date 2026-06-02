package com.example.auth

import android.content.Context
import android.net.Uri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleUser(
    val displayName: String,
    val email: String,
    val photoUrl: String,
)

class GoogleAuthManager(private val activityContext: Context) {

    private val credentialManager = CredentialManager.create(activityContext)
    private val webClientId = activityContext.getString(R.string.default_web_client_id)

    suspend fun signIn(filterAuthorizedAccounts: Boolean = false): Result<GoogleUser> =
        withContext(Dispatchers.IO) {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(filterAuthorizedAccounts)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext,
                )

                val credential = result.credential
                if (credential !is CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    return@withContext Result.failure(
                        IllegalStateException("Unexpected credential type")
                    )
                }

                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase sign in failed")

                val displayName = firebaseUser.displayName ?: googleCredential.displayName
                    ?: listOfNotNull(googleCredential.givenName, googleCredential.familyName)
                        .joinToString(" ")
                        .trim()
                val email = firebaseUser.email ?: googleCredential.id
                val photoUrl = normalizeGooglePhotoUrl(firebaseUser.photoUrl ?: googleCredential.profilePictureUri)

                Result.success(
                    GoogleUser(
                        displayName = displayName.ifBlank { email.substringBefore("@") },
                        email = email,
                        photoUrl = photoUrl,
                    )
                )
            } catch (e: GetCredentialCancellationException) {
                Result.failure(e)
            } catch (e: GoogleIdTokenParsingException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                FirebaseAuth.getInstance().signOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // Best-effort clear for next sign-in picker
            }
        }
    }

    private fun normalizeGooglePhotoUrl(uri: Uri?): String {
        val url = uri?.toString().orEmpty()
        if (url.isEmpty()) return ""
        if (!url.contains("googleusercontent.com")) return url
        val base = url.substringBefore('=')
        return "$base=s192-c"
    }
}
