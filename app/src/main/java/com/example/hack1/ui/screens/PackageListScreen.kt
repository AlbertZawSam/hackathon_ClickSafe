package com.example.hack1.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.hack1.NotificationRepository
import com.example.hack1.ui.components.PackageCard

@Composable
fun PackageListScreen(
    modifier: Modifier,
    onPackageClick: (String) -> Unit
) {

    val grouped = NotificationRepository.groupedNotifications
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (grouped.isEmpty()) {

            item {
                Text(
                    "No blacklisted notifications captured yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            items(grouped.keys.toList()) { pkg ->

                PackageCard(
                    packageName = pkg,
                    count = grouped[pkg]?.size ?: 0,
                    icon = getAppIcon(context, pkg),
                    onClick = { onPackageClick(pkg) }
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

fun getAppIcon(context: android.content.Context, packageName: String): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}