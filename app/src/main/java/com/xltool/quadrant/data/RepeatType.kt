package com.xltool.quadrant.data

/**
 * 任务重复类型
 */
enum class RepeatType(val value: Int, val displayName: String) {
    ONCE(0, "单次"),
    DAILY(1, "每天"),
    WEEKLY(2, "每周"),
    MONTHLY(3, "每月");

    companion object {
        fun fromValue(value: Int): RepeatType {
            return entries.firstOrNull { it.value == value } ?: ONCE
        }
    }
}
