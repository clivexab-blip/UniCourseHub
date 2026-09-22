package com.unicoursehub.app.util

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.unicoursehub.app.R

object GoogleAuthHelper {

    /**
     * Builds the Google Sign-In client used for the "Continue with Google" buttons.
     *
     * Requires a real app/google-services.json from your own Firebase project
     * (with the Google provider enabled) — see SETUP_FIREBASE.md. Until then,
     * R.string.default_web_client_id resolves to a placeholder and the Google
     * button will fail with a developer-console error, which is expected.
     */
    fun client(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
}
