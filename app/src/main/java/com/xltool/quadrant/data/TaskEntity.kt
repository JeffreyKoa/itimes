package com.xltool.quadrant.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["quadrant"]),
        Index(value = ["quadrant", "isPinned"]),
        Index(value = ["quadrant", "sortOrder"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val estimatedMinutes: Int?,
    /**
     * 截止日期（epochDay），例如 2025-12-12 => LocalDate.toEpochDay()
     * @deprecated 使用 dueTimestamp 代替
     */
    val dueEpochDay: Long?,
    /**
     * 精确截止时间（毫秒时间戳），包含年月日时分秒
     * 如果为 null 则使用 dueEpochDay（兼容旧数据）
     */
    val dueTimestamp: Long? = null,
    /**
     * 逗号分隔标签（MVP 简化，不做单独表）
     */
    val tags: String,
    val quadrant: Int,
    val status: Int,
    val isPinned: Boolean,
    /**
     * 是否开启提醒
     */
    val reminderEnabled: Boolean = false,
    /**
     * 提醒间隔数字（例如 3）
     */
    val reminderIntervalValue: Int? = null,
    /**
     * 提醒间隔单位（0=分钟，1=小时）
     */
    val reminderIntervalUnit: Int = 0,
    /**
     * 同象限内排序权重，越小越靠前（但 pinned 总优先）
     */
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    /**
     * 是否是今日最重要的任务（MIT - Most Important Task）
     * 同一时间只能有一个任务是 MIT
     */
    val isMIT: Boolean = false,
    /**
     * 录音文件路径（本地存储路径）
     */
    val audioPath: String? = null,
    /**
     * 重复类型：0=单次, 1=每天, 2=每周, 3=每月
     */
    val repeatType: Int = 0
)


