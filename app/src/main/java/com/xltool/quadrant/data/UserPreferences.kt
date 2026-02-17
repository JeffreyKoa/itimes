package com.xltool.quadrant.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_RUN, value).apply()

    var birthDate: Long
        get() = prefs.getLong(KEY_BIRTH_DATE, 0L)
        set(value) = prefs.edit().putLong(KEY_BIRTH_DATE, value).apply()

    companion object {
        private const val KEY_IS_FIRST_RUN = "is_first_run"
        private const val KEY_BIRTH_DATE = "birth_date"
    }
}
