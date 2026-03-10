package com.example.hack1.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    modifier: Modifier,
    isListenerEnabled: Boolean,
    hasPostPermission: Boolean,
    onOpenSettings: () -> Unit,
    onCheckPermission: () -> Unit,
    onRequestPostPermission: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        if (!isListenerEnabled) {
            Text(
                "Notification Access Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "This app needs access to read notifications to detect scams.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = onOpenSettings) {
                Text("Grant Access")
            }
        }

        if (!hasPostPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            Spacer(Modifier.height(24.dp))

            Text(
                "Notification Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "This app needs permission to alert you when a scam is detected.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(onClick = onRequestPostPermission) {
                Text("Grant Permission")
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = onCheckPermission) {
            Text("Refresh Status")
        }
    }
}