package com.example.yonosbi.services

import android.content.Context
import android.media.MediaDrm
import android.provider.Settings
import android.util.Base64
import java.util.UUID

object DeviceService {
    /**
     * Returns a unique and persistent device ID.
     * 1. Tries MediaDrm ID (hardware-backed, survives data clear)
     * 2. Falls back to ANDROID_ID (survives data clear and app re-installs)
     * 3. Final fallback: Random UUID (only if everything else fails)
     */
    fun getUniqueId(context: Context): String {
        // MediaDrm Unique ID (Hardware-backed, extremely persistent)
        val mediaDrmId = getMediaDrmId()
        if (!mediaDrmId.isNullOrBlank()) return mediaDrmId

        // ANDROID_ID (Scoped to app signing key + user + device)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        if (!androidId.isNullOrBlank()) return androidId

        return "unknown_device_${UUID.randomUUID()}"
    }

    private fun getMediaDrmId(): String? {
        return try {
            val widevineUuid = UUID(-0x121074568629b532L, -0x35b3d5440ecaa27dL)
            val mediaDrm = MediaDrm(widevineUuid)
            val androidIdByte = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            mediaDrm.release()
            Base64.encodeToString(androidIdByte, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}