package com.bunnyslist.android.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bunnyslist.android.R

/**
 * Gate shown until Firebase Auth has a signed-in user. The grocery list is
 * not composed behind this screen.
 */
@Composable
fun SignInScreen(
    isSigningIn: Boolean,
    errorMessage: String?,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_bunnys_logo),
            contentDescription = null,
            modifier = Modifier.height(72.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Bunny's Grocery List",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in with Google to see your list.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onSignIn,
            enabled = !isSigningIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Sign in with Google")
            }
        }
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Credential Manager needs the Activity, not the application context. */
internal fun Context.findActivity(): Activity {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    error("Sign-in requires an Activity context.")
}
