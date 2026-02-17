package com.xltool.quadrant.data

enum class Quadrant(val value: Int, val displayName: String) {
    IMPORTANT_URGENT(1, "重要紧急"),
    IMPORTANT_NOT_URGENT(2, "重要不紧急"),
    URGENT_NOT_IMPORTANT(3, "紧急不重要"),
    NOT_IMPORTANT_NOT_URGENT(4, "不重要不紧急");

    companion object {
        fun fromValue(value: Int): Quadrant {
            return entries.firstOrNull { it.value == value } ?: IMPORTANT_URGENT
        }
    }
}


