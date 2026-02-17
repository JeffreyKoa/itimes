package com.xltool.quadrant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        """
        SELECT * FROM tasks
        WHERE quadrant = :quadrant
        ORDER BY isPinned DESC, sortOrder ASC, updatedAt DESC
        """
    )
    fun observeByQuadrant(quadrant: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query(
        """
        SELECT * FROM tasks
        WHERE quadrant = :quadrant
        ORDER BY isPinned DESC, sortOrder ASC, updatedAt DESC
        """
    )
    suspend fun getOrderedByQuadrantOnce(quadrant: Int): List<TaskEntity>

    @Query("SELECT MIN(sortOrder) FROM tasks WHERE quadrant = :quadrant AND isPinned = 1")
    suspend fun minPinnedSortOrder(quadrant: Int): Long?

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE quadrant = :quadrant AND isPinned = 0")
    suspend fun maxUnpinnedSortOrder(quadrant: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    // MIT 相关查询
    @Query("SELECT * FROM tasks WHERE isMIT = 1 AND status != 1 LIMIT 1")
    fun observeMIT(): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE isMIT = 1 AND status != 1 LIMIT 1")
    suspend fun getMIT(): TaskEntity?

    @Query("UPDATE tasks SET isMIT = 0 WHERE isMIT = 1")
    suspend fun clearAllMIT()

    @Query("UPDATE tasks SET isMIT = 1 WHERE id = :id")
    suspend fun setMIT(id: Long)

    // 所有未完成任务（用于今日决策统计）
    @Query("SELECT * FROM tasks WHERE status != 1")
    fun observeAllActiveTasks(): Flow<List<TaskEntity>>

    // 按象限查询今天的任务（截止日期是今天或之前，或者没有截止日期，或者创建时间是今天）
    @Query(
        """
        SELECT * FROM tasks
        WHERE quadrant = :quadrant 
        AND status != 1
        AND (
            dueEpochDay IS NULL 
            OR dueEpochDay <= :todayEpochDay
            OR (dueTimestamp IS NOT NULL AND dueTimestamp <= :todayEndMillis)
            OR (createdAt >= :todayStartMillis AND createdAt <= :todayEndMillis)
        )
        ORDER BY isPinned DESC, sortOrder ASC, updatedAt DESC
        """
    )
    fun observeTodayByQuadrant(quadrant: Int, todayEpochDay: Long, todayStartMillis: Long, todayEndMillis: Long): Flow<List<TaskEntity>>

    // 按日期范围查询所有任务
    @Query(
        """
        SELECT * FROM tasks
        WHERE (
            (dueEpochDay IS NOT NULL AND dueEpochDay >= :startEpochDay AND dueEpochDay <= :endEpochDay)
            OR (dueTimestamp IS NOT NULL AND dueTimestamp >= :startMillis AND dueTimestamp <= :endMillis)
        )
        ORDER BY dueTimestamp ASC, dueEpochDay ASC, quadrant ASC
        """
    )
    fun observeTasksByDateRange(startEpochDay: Long, endEpochDay: Long, startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    // 查询所有任务（包括未来的）
    @Query("SELECT * FROM tasks ORDER BY dueTimestamp ASC, dueEpochDay ASC, quadrant ASC, updatedAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    // 查询需要提醒的任务（提醒已启用且截止时间在提醒范围内）
    @Query(
        """
        SELECT * FROM tasks
        WHERE status != 1 
        AND reminderEnabled = 1
        AND dueTimestamp IS NOT NULL
        ORDER BY dueTimestamp ASC
        """
    )
    fun observeTasksWithReminder(): Flow<List<TaskEntity>>

    // ========== 统计查询 ==========

    // 按时间范围统计任务（用于复盘）
    @Query(
        """
        SELECT * FROM tasks
        WHERE createdAt >= :startMillis AND createdAt <= :endMillis
        """
    )
    fun observeTasksByCreatedRange(startMillis: Long, endMillis: Long): Flow<List<TaskEntity>>

    // 查询所有任务（用于统计）
    @Query("SELECT * FROM tasks")
    fun observeAllTasksForStats(): Flow<List<TaskEntity>>

    // 查询已超过截止日期且状态不是已完成/已过期的任务
    @Query(
        """
        SELECT * FROM tasks
        WHERE status NOT IN (1, 2)
        AND dueTimestamp IS NOT NULL
        AND dueTimestamp < :currentTime
        """
    )
    suspend fun getOverdueTasks(currentTime: Long): List<TaskEntity>

    // 批量更新任务状态为已过期
    @Query("UPDATE tasks SET status = 2, updatedAt = :now WHERE id IN (:ids)")
    suspend fun updateTasksToOverdue(ids: List<Long>, now: Long)
}


