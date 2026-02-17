package com.xltool.quadrant.data

enum class TaskStatus(val value: Int, val displayName: String) {
    IN_PROGRESS(0, "进行中"),
    COMPLETED(1, "已完成"),
    OVERDUE(2, "已过期");

    companion object {
        fun fromValue(value: Int): TaskStatus {
            return entries.firstOrNull { it.value == value } ?: IN_PROGRESS
        }
    }
}
