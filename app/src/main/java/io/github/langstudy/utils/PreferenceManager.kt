package io.github.langstudy.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("langstudy_prefs", Context.MODE_PRIVATE)

    fun updateLastUsage() {
        prefs.edit().putLong(KEY_LAST_USAGE, System.currentTimeMillis()).apply()
    }

    fun getLastUsage(): Long {
        return prefs.getLong(KEY_LAST_USAGE, 0L)
    }

    companion object {
        private const val KEY_LAST_USAGE = "last_usage"
    }
}
