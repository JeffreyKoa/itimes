package com.xltool.quadrant.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

import java.time.ZoneId

data class TaskUiModel(
    val id: Long,
    val title: String,
    val description: String,
    val estimatedMinutes: Int?,
    val dueEpochDay: Long?,
    val dueTimestamp: Long?,  // 精确截止时间（毫秒）
    val tags: String,
    val quadrant: Quadrant,
    val status: TaskStatus,
    val isPinned: Boolean,
    val sortOrder: Long,
    val createdAt: Long,  // 创建时间（毫秒）
    val isMIT: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderIntervalValue: Int? = null,
    val reminderIntervalUnit: ReminderUnit = ReminderUnit.MINUTES,
    val audioPath: String? = null,
    val repeatType: RepeatType = RepeatType.ONCE
) {
    /**
     * 获取有效的截止时间戳
     * 优先使用 dueTimestamp，如果没有则从 dueEpochDay 转换（设为当天 23:59:59）
     */
    fun getEffectiveDueTimestamp(): Long? {
        if (dueTimestamp != null) return dueTimestamp
        if (dueEpochDay != null) {
            val date = LocalDate.ofEpochDay(dueEpochDay)
            return date.atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
        return null
    }

    /**
     * 获取提醒时间戳（截止时间 - 提前提醒时间）
     */
    fun getReminderTimestamp(): Long? {
        if (!reminderEnabled || reminderIntervalValue == null) return null
        val dueTime = getEffectiveDueTimestamp() ?: return null
        val reminderOffsetMillis = when (reminderIntervalUnit) {
            ReminderUnit.MINUTES -> reminderIntervalValue * 60 * 1000L
            ReminderUnit.HOURS -> reminderIntervalValue * 60 * 60 * 1000L
            ReminderUnit.DAYS -> reminderIntervalValue * 24 * 60 * 60 * 1000L
        }
        return dueTime - reminderOffsetMillis
    }

    /**
     * 是否应该显示提醒（动态提醒）
     * 条件：
     * 1. 提醒已启用
     * 2. 任务未完成（已完成的不显示）
     * 3. 当前时间 >= 提醒时间（截止时间 - 提前量）
     * 4. 如果已过期，最多显示24小时后消失
     * 
     * 注意：已过期的任务（status == OVERDUE）仍然显示提醒，直到过期超过24小时
     */
    fun shouldShowReminder(): Boolean {
        if (!reminderEnabled || reminderIntervalValue == null) return false
        // 已完成的任务不显示提醒
        if (status == TaskStatus.COMPLETED) return false
        
        val now = System.currentTimeMillis()
        val reminderTime = getReminderTimestamp() ?: return false
        val dueTime = getEffectiveDueTimestamp() ?: return false
        
        // 到了提醒时间才显示
        if (now < reminderTime) return false
        
        // 如果已过期超过24小时，不再显示提醒
        if (now > dueTime + 24 * 60 * 60 * 1000L) return false
        
        return true
    }

    /**
     * 是否已过期（当前时间超过截止时间）
     */
    fun isOverdue(): Boolean {
        val dueTime = getEffectiveDueTimestamp() ?: return false
        return System.currentTimeMillis() > dueTime
    }

    /**
     * 是否过期超过24小时
     */
    fun isOverdueMoreThan24Hours(): Boolean {
        val dueTime = getEffectiveDueTimestamp() ?: return false
        val now = System.currentTimeMillis()
        return now > dueTime + 24 * 60 * 60 * 1000L
    }

    /**
     * 获取过期时长（小时）
     */
    fun getOverdueHours(): Long {
        val dueTime = getEffectiveDueTimestamp() ?: return 0
        val now = System.currentTimeMillis()
        if (now <= dueTime) return 0
        return (now - dueTime) / (1000 * 60 * 60)
    }

    /**
     * 获取提醒状态描述
     */
    fun getReminderDescription(): String {
        if (!reminderEnabled || reminderIntervalValue == null) return ""
        return "提前 $reminderIntervalValue ${reminderIntervalUnit.displayName}提醒"
    }
}

data class TaskDraft(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    val estimatedMinutes: Int? = null,
    val dueEpochDay: Long? = null,
    val dueTimestamp: Long? = null,  // 精确截止时间（毫秒）
    val tags: String = "",
    val quadrant: Quadrant = Quadrant.IMPORTANT_URGENT,
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val isPinned: Boolean = false,
    val isMIT: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderIntervalValue: Int? = null,
    val reminderIntervalUnit: ReminderUnit = ReminderUnit.MINUTES,
    val audioPath: String? = null,
    val repeatType: RepeatType = RepeatType.ONCE
)

class TaskRepository(
    private val db: AppDatabase
) {
    private val dao = db.taskDao()

    fun observeQuadrant(quadrant: Quadrant): Flow<List<TaskUiModel>> {
        return dao.observeByQuadrant(quadrant.value).map { list -> list.map { it.toUi() } }
    }

    suspend fun getTaskDraft(id: Long): TaskDraft? {
        return dao.getById(id)?.toDraft()
    }

    suspend fun upsert(draft: TaskDraft) {
        val now = System.currentTimeMillis()
        val normalizedTitle = draft.title.trim()
        require(normalizedTitle.isNotEmpty()) { "title cannot be empty" }

        db.withTransaction {
            if (draft.id == null) {
                val sortOrder = now
                dao.insert(
                    TaskEntity(
                        title = normalizedTitle,
                        description = draft.description.trim(),
                        estimatedMinutes = draft.estimatedMinutes,
                        dueEpochDay = draft.dueEpochDay,
                        dueTimestamp = draft.dueTimestamp,
                        tags = normalizeTags(draft.tags),
                        quadrant = draft.quadrant.value,
                        status = draft.status.value,
                        isPinned = draft.isPinned,
                        reminderEnabled = draft.reminderEnabled,
                        reminderIntervalValue = draft.reminderIntervalValue,
                        reminderIntervalUnit = draft.reminderIntervalUnit.value,
                        sortOrder = sortOrder,
                        createdAt = now,
                        updatedAt = now,
                        isMIT = draft.isMIT,
                        audioPath = draft.audioPath,
                        repeatType = draft.repeatType.value
                    )
                )
            } else {
                val existing = dao.getById(draft.id) ?: return@withTransaction
                dao.update(
                    existing.copy(
                        title = normalizedTitle,
                        description = draft.description.trim(),
                        estimatedMinutes = draft.estimatedMinutes,
                        dueEpochDay = draft.dueEpochDay,
                        dueTimestamp = draft.dueTimestamp,
                        tags = normalizeTags(draft.tags),
                        quadrant = draft.quadrant.value,
                        status = draft.status.value,
                        isPinned = draft.isPinned,
                        reminderEnabled = draft.reminderEnabled,
                        reminderIntervalValue = draft.reminderIntervalValue,
                        reminderIntervalUnit = draft.reminderIntervalUnit.value,
                        updatedAt = now,
                        isMIT = draft.isMIT,
                        audioPath = draft.audioPath,
                        repeatType = draft.repeatType.value
                    )
                )
            }
        }
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteAndReturn(id: Long): TaskEntity? {
        return db.withTransaction {
            val existing = dao.getById(id) ?: return@withTransaction null
            dao.deleteById(id)
            existing
        }
    }

    suspend fun restore(entity: TaskEntity) {
        db.withTransaction {
            dao.insert(entity)
        }
    }

    suspend fun moveToQuadrant(id: Long, target: Quadrant) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = dao.getById(id) ?: return@withTransaction
            if (existing.quadrant == target.value) return@withTransaction
            dao.update(
                existing.copy(
                    quadrant = target.value,
                    // 迁移后默认放到该象限末尾（较大 sortOrder）
                    sortOrder = now,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun setStatus(id: Long, status: TaskStatus) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = dao.getById(id) ?: return@withTransaction
            dao.update(existing.copy(status = status.value, updatedAt = now))
        }
    }

    /**
     * 自动检测并更新已超过截止日期的任务状态为"已过期"
     * 一旦超过截止日期，状态立即变为已过期
     * 返回更新的任务数量
     */
    suspend fun updateOverdueTasks(): Int {
        val now = System.currentTimeMillis()
        
        return db.withTransaction {
            val overdueTasks = dao.getOverdueTasks(now)
            if (overdueTasks.isNotEmpty()) {
                val ids = overdueTasks.map { it.id }
                dao.updateTasksToOverdue(ids, now)
            }
            overdueTasks.size
        }
    }

    /**
     * 推迟截止日期：若原本没有截止日期，则以"今天"为基准设置为 tomorrow。
     */
    suspend fun deferDueByDays(id: Long, days: Long) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = dao.getById(id) ?: return@withTransaction
            val base = existing.dueEpochDay ?: LocalDate.now().toEpochDay()
            dao.update(existing.copy(dueEpochDay = base + days, updatedAt = now))
        }
    }

    suspend fun togglePinned(id: Long) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val existing = dao.getById(id) ?: return@withTransaction
            val newPinned = !existing.isPinned
            val newSortOrder = if (newPinned) {
                val minPinned = dao.minPinnedSortOrder(existing.quadrant) ?: existing.sortOrder
                minPinned - 1
            } else {
                val maxUnpinned = dao.maxUnpinnedSortOrder(existing.quadrant) ?: existing.sortOrder
                maxUnpinned + 1
            }
            dao.update(existing.copy(isPinned = newPinned, sortOrder = newSortOrder, updatedAt = now))
        }
    }

    suspend fun moveUp(id: Long) {
        moveRelative(id, direction = -1)
    }

    suspend fun moveDown(id: Long) {
        moveRelative(id, direction = +1)
    }

    private suspend fun moveRelative(id: Long, direction: Int) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val target = dao.getById(id) ?: return@withTransaction
            val ordered = dao.getOrderedByQuadrantOnce(target.quadrant)
            // pinned 与非 pinned 分组，MVP 仅允许在同组内调整顺序，避免“置顶规则”引起排序错觉
            val sameGroup = ordered.filter { it.isPinned == target.isPinned }
            val index = sameGroup.indexOfFirst { it.id == id }
            if (index == -1) return@withTransaction
            val swapIndex = index + direction
            if (swapIndex !in sameGroup.indices) return@withTransaction

            val a = sameGroup[index]
            val b = sameGroup[swapIndex]
            dao.update(a.copy(sortOrder = b.sortOrder, updatedAt = now))
            dao.update(b.copy(sortOrder = a.sortOrder, updatedAt = now))
        }
    }

    private fun TaskEntity.toUi(): TaskUiModel {
        return TaskUiModel(
            id = id,
            title = title,
            description = description,
            estimatedMinutes = estimatedMinutes,
            dueEpochDay = dueEpochDay,
            dueTimestamp = dueTimestamp,
            tags = tags,
            quadrant = Quadrant.fromValue(quadrant),
            status = TaskStatus.fromValue(status),
            isPinned = isPinned,
            sortOrder = sortOrder,
            createdAt = createdAt,
            isMIT = isMIT,
            reminderEnabled = reminderEnabled,
            reminderIntervalValue = reminderIntervalValue,
            reminderIntervalUnit = ReminderUnit.fromValue(reminderIntervalUnit),
            audioPath = audioPath,
            repeatType = RepeatType.fromValue(repeatType)
        )
    }

    private fun TaskEntity.toDraft(): TaskDraft {
        return TaskDraft(
            id = id,
            title = title,
            description = description,
            estimatedMinutes = estimatedMinutes,
            dueEpochDay = dueEpochDay,
            dueTimestamp = dueTimestamp,
            tags = tags,
            quadrant = Quadrant.fromValue(quadrant),
            status = TaskStatus.fromValue(status),
            isPinned = isPinned,
            isMIT = isMIT,
            reminderEnabled = reminderEnabled,
            reminderIntervalValue = reminderIntervalValue,
            reminderIntervalUnit = ReminderUnit.fromValue(reminderIntervalUnit),
            audioPath = audioPath,
            repeatType = RepeatType.fromValue(repeatType)
        )
    }

    private fun normalizeTags(input: String): String {
        return input
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
    }

    // ========== MIT 相关方法 ==========

    /**
     * 观察当前 MIT 任务
     */
    fun observeMIT(): Flow<TaskUiModel?> {
        return dao.observeMIT().map { it?.toUi() }
    }

    /**
     * 观察所有活跃任务（用于今日决策统计）
     */
    fun observeAllActiveTasks(): Flow<List<TaskUiModel>> {
        return dao.observeAllActiveTasks().map { list -> list.map { it.toUi() } }
    }

    /**
     * 设置任务为 MIT（会自动清除之前的 MIT）
     */
    suspend fun setMIT(id: Long) {
        db.withTransaction {
            dao.clearAllMIT()
            dao.setMIT(id)
        }
    }

    /**
     * 清除 MIT
     */
    suspend fun clearMIT() {
        dao.clearAllMIT()
    }

    /**
     * 获取建议的 MIT（从重要不紧急象限中选择第一个未完成任务）
     */
    suspend fun getSuggestedMIT(): TaskUiModel? {
        val tasks = dao.getOrderedByQuadrantOnce(Quadrant.IMPORTANT_NOT_URGENT.value)
        return tasks.firstOrNull { TaskStatus.fromValue(it.status) != TaskStatus.COMPLETED }?.toUi()
    }

    // ========== 今日任务查询 ==========

    /**
     * 观察今天的任务（按象限）
     * 只显示截止日期是今天或之前的任务
     */
    fun observeTodayQuadrant(quadrant: Quadrant): Flow<List<TaskUiModel>> {
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val todayStartMillis = today.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val todayEndMillis = today.atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return dao.observeTodayByQuadrant(quadrant.value, todayEpochDay, todayStartMillis, todayEndMillis)
            .map { list -> list.map { it.toUi() } }
    }

    // ========== 任务查询功能 ==========

    /**
     * 观察所有任务
     */
    fun observeAllTasks(): Flow<List<TaskUiModel>> {
        return dao.observeAllTasks().map { list -> list.map { it.toUi() } }
    }

    /**
     * 按日期范围查询任务
     */
    fun observeTasksByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskUiModel>> {
        val startEpochDay = startDate.toEpochDay()
        val endEpochDay = endDate.toEpochDay()
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.observeTasksByDateRange(startEpochDay, endEpochDay, startMillis, endMillis)
            .map { list -> list.map { it.toUi() } }
    }

    // ========== 提醒功能 ==========

    /**
     * 观察需要提醒的任务
     */
    fun observeTasksWithReminder(): Flow<List<TaskUiModel>> {
        return dao.observeTasksWithReminder().map { list -> list.map { it.toUi() } }
    }

    // ========== 统计功能 ==========

    /**
     * 观察指定时间范围内创建的任务
     */
    fun observeTasksByCreatedRange(startMillis: Long, endMillis: Long): Flow<List<TaskUiModel>> {
        return dao.observeTasksByCreatedRange(startMillis, endMillis).map { list -> list.map { it.toUi() } }
    }

    /**
     * 观察所有任务（用于统计）
     */
    fun observeAllTasksForStats(): Flow<List<TaskUiModel>> {
        return dao.observeAllTasksForStats().map { list -> list.map { it.toUi() } }
    }
}


