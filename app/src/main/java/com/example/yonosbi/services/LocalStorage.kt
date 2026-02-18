package com.example.yonosbi.services

import android.content.Context
import android.content.SharedPreferences
import com.example.yonosbi.utils.Constants

class LocalStorage(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    fun setItem(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    fun getItem(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun removeItem(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
