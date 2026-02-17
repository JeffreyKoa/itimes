package com.xltool.quadrant.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val MD_CN: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

fun formatEpochDay(epochDay: Long): String {
    return LocalDate.ofEpochDay(epochDay).format(ISO_DATE)
}

fun parseIsoLocalDateOrNull(input: String): LocalDate? {
    val text = input.trim()
    if (text.isEmpty()) return null
    return try {
        LocalDate.parse(text, ISO_DATE)
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * 更适合“任务列表”展示的短日期（例如：12月20日）。
 */
fun formatEpochDayShortCn(epochDay: Long): String {
    return LocalDate.ofEpochDay(epochDay).format(MD_CN)
}


