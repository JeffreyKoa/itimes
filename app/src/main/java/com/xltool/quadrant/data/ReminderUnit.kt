package com.xltool.quadrant.data

import java.util.concurrent.TimeUnit

enum class ReminderUnit(
    val value: Int,
    val displayName: String
) {
    MINUTES(0, "分钟"),
    HOURS(1, "小时"),
    DAYS(2, "天");

    fun toMillis(intervalValue: Int): Long {
        return when (this) {
            MINUTES -> TimeUnit.MINUTES.toMillis(intervalValue.toLong())
            HOURS -> TimeUnit.HOURS.toMillis(intervalValue.toLong())
            DAYS -> TimeUnit.DAYS.toMillis(intervalValue.toLong())
        }
    }

    companion object {
        fun fromValue(value: Int): ReminderUnit {
            return entries.firstOrNull { it.value == value } ?: MINUTES
        }
    }
}


