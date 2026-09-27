package com.example.multitrans

import android.content.Context
import android.content.SharedPreferences

class LanguagePreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("multi_trans_prefs", Context.MODE_PRIVATE)

    fun savePreferredLanguages(languages: List<String>) {
        prefs.edit().putString("preferred_langs", languages.joinToString(",")).apply()
    }

    fun getPreferredLanguages(): List<String> {
        val saved = prefs.getString("preferred_langs", null)
        return if (saved.isNullOrEmpty()) {
            // Default list if none saved
            listOf("en", "fr", "es", "ca", "it", "el", "de", "ar")
        } else {
            saved.split(",")
        }
    }
}
