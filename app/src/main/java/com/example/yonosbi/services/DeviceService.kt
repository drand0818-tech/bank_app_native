package com.example.yonosbi.services

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object DeviceService {

    /**
     * Returns a persistent device-level ID for all Android versions.
     *
     * - Android < 10: uses ANDROID_ID (device + signing key)
     * - Android ≥ 10: uses stable hardware fingerprint
     * - Fallback: random UUID (extremely rare)
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val idSource: String = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Use ANDROID_ID for older Android versions
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.ifBlank { generateHardwareFingerprint() } ?: generateHardwareFingerprint()
        } else {
            // Use hardware fingerprint for Android 10+
            generateHardwareFingerprint()
        }

        return sha256(idSource)
    }

    /**
     * Generates a pseudo device fingerprint using stable hardware properties.
     * Fully offline and does not rely on deprecated SERIAL or ANDROID_ID.
     */
    private fun generateHardwareFingerprint(): String {
        val fingerprintSource = listOf(
            Build.MANUFACTURER,
            Build.MODEL,
            Build.BOARD,
            Build.HARDWARE,
            Build.FINGERPRINT
        ).joinToString("-")
            .ifBlank { UUID.randomUUID().toString() } // fallback extremely rare

        return fingerprintSource
    }

    /**
     * SHA-256 hash function for privacy
     */
    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
