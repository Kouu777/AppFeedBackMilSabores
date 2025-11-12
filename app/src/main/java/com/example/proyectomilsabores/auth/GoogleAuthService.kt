package com.example.proyectomilsabores.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.tasks.await

class GoogleAuthService(private val context: Context) {
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)


    suspend fun getSignInPendingIntent(): Result<android.app.PendingIntent> {
        return try {
            val signInResult = oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
            Result.success(signInResult.pendingIntent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun handleSignInResult(intent: Intent): Result<String> {
        return try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                Result.success(idToken)
            } else {
                Result.failure(Exception("No se pudo obtener el token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId("tu-client-id-aqui")
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}