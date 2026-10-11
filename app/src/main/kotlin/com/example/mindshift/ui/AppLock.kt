package com.example.mindshift.ui

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun canUseBiometric(context: Context): Boolean {
    val authenticators = if (Build.VERSION.SDK_INT >= 30)
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    else
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    return BiometricManager.from(context).canAuthenticate(authenticators) ==
        BiometricManager.BIOMETRIC_SUCCESS
}

private fun authenticatorsForDevice(): Int =
    if (Build.VERSION.SDK_INT >= 30)
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    else
        BiometricManager.Authenticators.BIOMETRIC_WEAK

@Composable
fun LockOverlay(onUnlock: () -> Unit, onDisableLock: () -> Unit) {
    val context = LocalContext.current
    val biometricOk = remember { canUseBiometric(context) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun authenticate() {
        val activity = context as? FragmentActivity ?: return
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MindShift")
            .setAllowedAuthenticators(authenticatorsForDevice())
        if (Build.VERSION.SDK_INT < 30) builder.setNegativeButtonText("Cancel")

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlock()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) errorText = errString.toString()
                }
            }
        )
        runCatching { prompt.authenticate(builder.build()) }
    }

    LaunchedEffect(Unit) { if (biometricOk) authenticate() }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.Lock, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text("MindShift is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                errorText ?: "Use your fingerprint, face, or screen lock to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            if (biometricOk) {
                Button(onClick = { authenticate() }) { Text("Unlock") }
            } else {
                Text(
                    "No biometric or screen lock is set up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onDisableLock) { Text("Turn off app lock") }
            }
        }
    }
}
