package com.example.hack1

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.hack1.ui.components.MainTopBar
import com.example.hack1.ui.screens.NotificationListScreen
import com.example.hack1.ui.screens.PackageListScreen
import com.example.hack1.ui.screens.PermissionScreen
import com.example.hack1.ui.theme.Hack1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationRepository.init(this)

        setContent {
            Hack1Theme {
                var isListenerEnabled by remember {
                    mutableStateOf(isNotificationServiceEnabled())
                }

                var hasPostNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                var selectedPackage by remember {
                    mutableStateOf<String?>(null)
                }

                LaunchedEffect(intent) {
                    intent?.getStringExtra("package_name")?.let { pkg ->
                        selectedPackage = pkg
                    }
                }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasPostNotificationPermission = isGranted
                    }

                LaunchedEffect(Unit) {
                    if (!isListenerEnabled) {
                        openNotificationSettings()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasPostNotificationPermission
                    ) {
                        permissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }

                BackHandler(enabled = selectedPackage != null) {
                    selectedPackage = null
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MainTopBar(
                            isPermissionEnabled = isListenerEnabled && hasPostNotificationPermission,
                            selectedPackage = selectedPackage,
                            onBack = { selectedPackage = null }
                        )
                    }
                ) { innerPadding ->
                    if (!isListenerEnabled || !hasPostNotificationPermission) {
                        PermissionScreen(
                            modifier = Modifier.padding(innerPadding),
                            isListenerEnabled = isListenerEnabled,
                            hasPostPermission = hasPostNotificationPermission,
                            onOpenSettings = { openNotificationSettings() },
                            onCheckPermission = {
                                isListenerEnabled = isNotificationServiceEnabled()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    hasPostNotificationPermission =
                                        ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                }
                            },
                            onRequestPostPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                }
                            }
                        )
                    } else if (selectedPackage == null) {
                        PackageListScreen(
                            modifier = Modifier.padding(innerPadding),
                            onPackageClick = { selectedPackage = it }
                        )
                    } else {
                        NotificationListScreen(
                            modifier = Modifier.padding(innerPadding),
                            packageName = selectedPackage!!
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        )
    }
}
