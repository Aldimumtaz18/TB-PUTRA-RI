package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
  private val auth: FirebaseAuth = Firebase.auth
  private val credentialManager: CredentialManager = CredentialManager.create(context)

  val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
    val listener =
      FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
      }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  suspend fun attemptSilentSignIn(): FirebaseUser? {
    if (auth.currentUser != null) return auth.currentUser
    val webClientId = context.getString(R.string.default_web_client_id)
    if (webClientId.isBlank()) return null

    return try {
      val googleIdOption =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(true)
          .setServerClientId(webClientId)
          .setAutoSelectEnabled(true)
          .build()

      val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
      val result = credentialManager.getCredential(context = context, request = request)
      signInWithCredentialResult(result.credential).getOrNull()
    } catch (_: NoCredentialException) {
      null
    } catch (_: GetCredentialCancellationException) {
      null
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Silent Google sign-in skipped: ${e.message}")
      null
    }
  }

  suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
    val webClientId = context.getString(R.string.default_web_client_id)
    if (webClientId.isBlank()) {
      return Result.failure(IllegalStateException("Web Client ID Google belum tersedia."))
    }

    return try {
      val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
      val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
      val result = credentialManager.getCredential(context = activityContext, request = request)
      signInWithCredentialResult(result.credential)
    } catch (e: CancellationException) {
      throw e
    } catch (e: GetCredentialCancellationException) {
      Log.i(TAG, "Google sign-in cancelled by user")
      Result.failure(IllegalStateException("Login Google dibatalkan."))
    } catch (e: NoCredentialException) {
      Log.w(TAG, "No Google credential available", e)
      Result.failure(
        IllegalStateException("Tidak ada akun Google aktif di perangkat. Silakan tambahkan akun Google.")
      )
    } catch (e: GetCredentialException) {
      Log.e(TAG, "Credential Manager error", e)
      Result.failure(
        IllegalStateException("Gagal masuk dengan Google: ${e.localizedMessage ?: "periksa koneksi."}")
      )
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected sign-in error", e)
      Result.failure(IllegalStateException(e.localizedMessage ?: "Gagal login ke Google."))
    }
  }

  private suspend fun signInWithCredentialResult(
    credential: androidx.credentials.Credential
  ): Result<FirebaseUser> {
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      return try {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCred = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        val authResult = auth.signInWithCredential(firebaseCred).await()
        val user =
          authResult.user ?: return Result.failure(IllegalStateException("Akun Firebase tidak ditemukan."))
        Result.success(user)
      } catch (e: CancellationException) {
        throw e
      } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Invalid Google ID token", e)
        Result.failure(IllegalStateException("Token akun Google tidak valid."))
      } catch (e: Exception) {
        Log.e(TAG, "Firebase signInWithCredential failed", e)
        Result.failure(IllegalStateException(e.localizedMessage ?: "Gagal menghubungkan ke Firebase."))
      }
    }
    return Result.failure(IllegalStateException("Kredensial yang dipilih bukan akun Google."))
  }

  suspend fun signOut(beforeSignOut: suspend () -> Unit = {}) {
    withContext(NonCancellable) {
      runCatching { beforeSignOut() }
      auth.signOut()
      runCatching {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      }
    }
  }

  private companion object {
    const val TAG = "AuthRepository"
  }
}
