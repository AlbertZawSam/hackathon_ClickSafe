package com.example.hack1.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hack1.NotificationData
import com.example.hack1.NotificationRepository
import com.example.hack1.ui.components.NotificationCard

@Composable
fun NotificationListScreen(
    modifier: Modifier,
    packageName: String
) {

    val notifications =
        NotificationRepository.groupedNotifications[packageName]
            ?: emptyList<NotificationData>()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        items(notifications) { notification ->

            NotificationCard(notification)

            Spacer(Modifier.height(8.dp))
        }
    }
}