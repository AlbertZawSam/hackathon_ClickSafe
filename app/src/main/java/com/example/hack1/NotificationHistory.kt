package com.example.hack1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.regex.Pattern

data class NotificationData(
    val key: String,
    val packageName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pendingLinks: List<String> = emptyList() // Links that triggered a suspicious match but aren't in blacklist yet
)

object NotificationRepository {
    val groupedNotifications = mutableStateMapOf<String, MutableList<NotificationData>>()
    
    private val staticBlacklist = mutableSetOf<String>()
    private val dynamicBlacklist = mutableSetOf<String>()
    private lateinit var dynamicFile: File
    private const val CHANNEL_ID = "scam_alerts"

    private val suspiciousPatterns = listOf(
        Regex("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*"), // IP addresses
        Regex("https?://.*(bit\\.ly|t\\.co|tinyurl\\.com|ow\\.ly).*"), // Link shorteners
        Regex("https?://.*(login|verify|account|secure|update|bank|password).*"), // Keywords
        Regex("https?://.*\\.(xyz|top|info|tk|ml|ga|cf|gq)$") // Suspicious TLDs
    )

    private val urlPattern = Pattern.compile(
        "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)"
                + "(([\\w\\-]+\\.){1,}\\w+(:\\d+)?(\\/[\\w\\-./?%&=]*)?)"
    )

    fun init(context: Context) {
        createNotificationChannel(context)
        
        try {
            val inputStream = context.resources.openRawResource(R.raw.blacklist)
            staticBlacklist.addAll(inputStream.bufferedReader().use { it.readLines() }.filter { it.isNotBlank() })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dynamicFile = File(context.filesDir, "dynamic_blacklist.txt")
        if (dynamicFile.exists()) {
            dynamicBlacklist.addAll(dynamicFile.readLines().filter { it.isNotBlank() })
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scam Alerts"
            val descriptionText = "Notifications for detected scams"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showScamNotification(context: Context, offendingPackage: String, isNew: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("package_name", offendingPackage)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            offendingPackage.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(if (isNew) "New Scam link detected" else "Scam detected")
            .setContentText(offendingPackage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(offendingPackage.hashCode(), builder.build())
    }

    private fun extractLinks(text: String): List<String> {
        val links = mutableListOf<String>()
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val urlStr = matcher.group(1) + matcher.group(4)
            links.add(urlStr)
        }
        return links
    }

    private fun isSuspicious(url: String): Boolean {
        return suspiciousPatterns.any { it.matches(url) }
    }

    fun addNotification(context: Context, notification: NotificationData) {
        val allText = "${notification.title} ${notification.content}"
        val foundLinks = extractLinks(allText)
        
        var isKnownScam = false
        val newSuspiciousLinks = mutableListOf<String>()

        for (link in foundLinks) {
            if (staticBlacklist.any { link.contains(it, ignoreCase = true) } || 
                dynamicBlacklist.any { link.contains(it, ignoreCase = true) }) {
                isKnownScam = true
            } else if (isSuspicious(link)) {
                newSuspiciousLinks.add(link)
            }
        }

        if (isKnownScam || newSuspiciousLinks.isNotEmpty()) {
            showScamNotification(context, notification.packageName, isNew = !isKnownScam && newSuspiciousLinks.isNotEmpty())

            val list = groupedNotifications.getOrPut(notification.packageName) { mutableStateListOf() }
            list.add(0, notification.copy(pendingLinks = newSuspiciousLinks))
            
            if (list.size > 100) {
                list.removeAt(list.size - 1)
            }
        }
    }

    fun confirmScam(context: Context, notification: NotificationData) {
        if (notification.pendingLinks.isNotEmpty()) {
            dynamicBlacklist.addAll(notification.pendingLinks)
            saveDynamicBlacklist()
            
            // Remove the pending status from the notification in UI
            val list = groupedNotifications[notification.packageName]
            val index = list?.indexOfFirst { it.key == notification.key && it.timestamp == notification.timestamp }
            if (index != null && index != -1) {
                list[index] = notification.copy(pendingLinks = emptyList())
            }

            Toast.makeText(context, "Links added to blacklist", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelScam(packageName: String, notification: NotificationData) {
        val list = groupedNotifications[packageName]
        list?.removeIf { it.key == notification.key && it.timestamp == notification.timestamp }
        if (list?.isEmpty() == true) {
            groupedNotifications.remove(packageName)
        }
    }

    private fun saveDynamicBlacklist() {
        try {
            if (dynamicFile.canWrite() || !dynamicFile.exists()) {
                dynamicFile.writeText(dynamicBlacklist.joinToString("\n"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun <T> mutableStateListOf() = androidx.compose.runtime.mutableStateListOf<T>()
