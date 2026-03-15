package com.sbi.yonosbi.utils

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.result.ActivityResultLauncher
import com.sbi.yonosbi.services.UserApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SmsUtils {
    private const val TAG = "SMS_DEBUG"

    fun isDefaultSmsApp(context: Context): Boolean {
        return when {
            // Android < 4.4
            Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT -> {
                android.util.Log.d(TAG, "SDK < 19 — default SMS not required")
                true
            }

            // Android 4.4 – 9
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
                val isDefault = defaultPackage == context.packageName
                android.util.Log.d(
                    TAG,
                    "SDK 19–28 — defaultSmsPackage=$defaultPackage, packageName=${context.packageName}, isDefault=$isDefault"
                )
                isDefault
            }

            // Android 10+
            else -> {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager == null) {
                    android.util.Log.e(
                        TAG,
                        "RoleManager returned NULL — cannot verify SMS role"
                    )
                    reportDiagnostic(
                        context = context,
                        event = "sms_rolemanager_null_verify"
                    )
                    false
                } else {
                    val isHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                    val isAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS)

                    android.util.Log.d(
                        TAG,
                        "SDK 29+ — ROLE_SMS available=$isAvailable, held=$isHeld, packageName=${context.packageName}"
                    )

                    isHeld
                }
            }
        }
    }

    // requesting function for request
    fun requestDefaultSmsApp(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>
    ) {

        android.util.Log.d(
            TAG,
            "requestDefaultSmsApp called — SDK=${Build.VERSION.SDK_INT}, manufacturer=${Build.MANUFACTURER}, model=${Build.MODEL}"
        )
        when {
            // Android < 4.4
            Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT -> {
                android.util.Log.d(
                    TAG,
                    "SDK < 19 — Default SMS concept not required. Proceeding."
                )

                return
            }
            // Android 4.4 – 9
            Build.VERSION.SDK_INT in Build.VERSION_CODES.KITKAT..Build.VERSION_CODES.P -> {
                android.util.Log.d(
                    TAG,
                    "SDK 19–28 — Requesting default SMS app via Telephony intent"
                )
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(
                    Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    activity.packageName
                )
                launcher.launch(intent)
            }

            // Android 10+
            else -> {
                val roleManager = activity.getSystemService(RoleManager::class.java)
                if (roleManager == null) {
                    android.util.Log.e(
                        TAG,
                        "RoleManager returned NULL — cannot request ROLE_SMS"
                    )
                    reportDiagnostic(
                        context = activity,
                        event = "sms_rolemanager_null_request"
                    )
                    launcher.launch(createDefaultAppsSettingsIntent())
                    return
                }
                if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    android.util.Log.e(
                        TAG,
                        "ROLE_SMS is unavailable on this device"
                    )
                    reportDiagnostic(
                        context = activity,
                        event = "sms_role_unavailable",
                        details = mapOf(
                            "sdk_int" to Build.VERSION.SDK_INT
                        )
                    )
                    launcher.launch(createDefaultAppsSettingsIntent())
                    return
                }
                val roleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                android.util.Log.d(
                    TAG,
                    "SDK 29+ — ROLE_SMS available=${roleManager.isRoleAvailable(RoleManager.ROLE_SMS)}, currently held=$roleHeld"
                )
                if (!roleHeld) {
                    android.util.Log.d(
                        TAG,
                        "Requesting ROLE_SMS from RoleManager"
                    )
                    try {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        launcher.launch(intent)
                    } catch (exception: Exception) {
                        android.util.Log.e(
                            TAG,
                            "Failed to launch ROLE_SMS request; opening default apps settings",
                            exception
                        )
                        reportDiagnostic(
                            context = activity,
                            event = "sms_role_request_launch_failed",
                            details = mapOf(
                                "exception" to (exception.message ?: exception.javaClass.simpleName)
                            )
                        )
                        launcher.launch(createDefaultAppsSettingsIntent())
                    }
                } else {
                    android.util.Log.d(
                        TAG,
                        "ROLE_SMS already granted — no request needed"
                    )
                }
            }
        }
    }

    private fun createDefaultAppsSettingsIntent(): Intent {
        android.util.Log.d(TAG, "Creating fallback intent for ACTION_MANAGE_DEFAULT_APPS_SETTINGS")
        return Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    }

    private fun reportDiagnostic(
        context: Context,
        event: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            UserApiService(context).reportClientDiagnostic(
                event = event,
                details = details
            )
        }
    }
}
