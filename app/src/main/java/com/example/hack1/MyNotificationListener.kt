package com.example.hack1

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            NotificationRepository.init(applicationContext)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val extras = sbn.notification?.extras
        val title = extras?.getString(Notification.EXTRA_TITLE) ?: "No Title"
        val content = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "No Content"
        val packageName = sbn.packageName ?: "Unknown"
        val key = sbn.key

        serviceScope.launch {
            Log.d("NotificationListener", "Processing: $packageName")
            
            NotificationRepository.addNotification(
                applicationContext,
                NotificationData(
                    key = key,
                    packageName = packageName,
                    title = title,
                    content = content
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
