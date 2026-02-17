package com.xltool.quadrant.ui.home

import android.content.Context
import android.media.MediaPlayer
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xltool.quadrant.R
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.TaskUiModel

/**
 * 播放自定义提示音
 */
private fun playNotificationSound(context: Context) {
    try {
        val mediaPlayer = MediaPlayer.create(context, R.raw.reminder_tone)
        mediaPlayer?.setOnCompletionListener { it.release() }
        mediaPlayer?.start()
    } catch (e: Exception) {
        // 忽略播放错误
    }
}

/**
 * 今日决策数据
 */
data class TodayDecisionState(
    val currentMIT: TaskUiModel? = null,
    val suggestedMIT: TaskUiModel? = null,
    val quadrantCounts: QuadrantCounts = QuadrantCounts(),
    val importantTasks: List<TaskUiModel> = emptyList(),
    // 各象限任务列表
    val q1Tasks: List<TaskUiModel> = emptyList(),
    val q2Tasks: List<TaskUiModel> = emptyList(),
    val q3Tasks: List<TaskUiModel> = emptyList(),
    val q4Tasks: List<TaskUiModel> = emptyList()
)

data class QuadrantCounts(
    val urgentImportant: Int = 0,
    val importantNotUrgent: Int = 0,
    val urgentNotImportant: Int = 0,
    val notUrgentNotImportant: Int = 0
) {
    val total: Int get() = urgentImportant + importantNotUrgent + urgentNotImportant + notUrgentNotImportant
}

/**
 * 系统提醒类型
 */
sealed class ReminderType(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val containerColor: Color,
    val contentColor: Color,
    val iconBackgroundColor: Color
) {
    data class Warning(val msg: String) : ReminderType(
        icon = Icons.Default.Warning,
        title = "时间分配风险",
        message = msg,
        containerColor = Color(0xFFFFF8E1),
        contentColor = Color(0xFFE65100),
        iconBackgroundColor = Color(0xFFFFE0B2)
    )

    data class Alert(val msg: String) : ReminderType(
        icon = Icons.Default.Warning,
        title = "任务过载警告",
        message = msg,
        containerColor = Color(0xFFFFEBEE),
        contentColor = Color(0xFFC62828),
        iconBackgroundColor = Color(0xFFFFCDD2)
    )

    data class Info(val msg: String) : ReminderType(
        icon = Icons.Default.Lightbulb,
        title = "长期目标提醒",
        message = msg,
        containerColor = Color(0xFFE3F2FD),
        contentColor = Color(0xFF1565C0),
        iconBackgroundColor = Color(0xFFBBDEFB)
    )

    data class Success(val msg: String) : ReminderType(
        icon = Icons.Default.CheckCircle,
        title = "状态良好",
        message = msg,
        containerColor = Color(0xFFE8F5E9),
        contentColor = Color(0xFF2E7D32),
        iconBackgroundColor = Color(0xFFC8E6C9)
    )

    data class Neutral(val msg: String) : ReminderType(
        icon = Icons.Default.TrendingUp,
        title = "今日概览",
        message = msg,
        containerColor = Color(0xFFF5F5F5),
        contentColor = Color(0xFF616161),
        iconBackgroundColor = Color(0xFFE0E0E0)
    )
}

/**
 * 根据象限分布生成系统提醒
 */
private fun generateReminder(counts: QuadrantCounts): ReminderType {
    return when {
        counts.urgentNotImportant > counts.importantNotUrgent -> {
            ReminderType.Warning("紧急不重要的任务过多，可能挤压真正重要的事项。建议清理或委托部分任务。")
        }
        counts.importantNotUrgent == 0 -> {
            ReminderType.Info("今日没有「重要不紧急」任务，别忘了投入时间在长期目标上。")
        }
        counts.urgentImportant > 5 -> {
            ReminderType.Alert("重要紧急任务超过5项，建议重新评估优先级，避免决策疲劳。")
        }
        counts.importantNotUrgent >= counts.urgentNotImportant -> {
            ReminderType.Success("任务分配合理，继续专注于重要的事情！")
        }
        else -> {
            ReminderType.Neutral("共 ${counts.total} 项待办任务，保持专注，逐个击破。")
        }
    }
}

// 颜色定义
private val MITGradientStart = Color(0xFF5C6BC0)
private val MITGradientEnd = Color(0xFF7E57C2)
private val QuadrantRedLight = Color(0xFFFFEBEE)
private val QuadrantRed = Color(0xFFE53935)
private val QuadrantBlueLight = Color(0xFFE3F2FD)
private val QuadrantBlue = Color(0xFF1E88E5)
private val QuadrantOrangeLight = Color(0xFFFFF3E0)
private val QuadrantOrange = Color(0xFFFB8C00)
private val QuadrantGrayLight = Color(0xFFF5F5F5)
private val QuadrantGray = Color(0xFF757575)


@Composable
fun TodayDecisionPanel(
    state: TodayDecisionState,
    tasksWithReminder: List<TaskUiModel>,
    onSetMIT: (Long) -> Unit,
    onClearMIT: () -> Unit,
    onEditTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onSetTaskStatus: (Long, TaskStatus) -> Unit,
    onCompleteTask: (Long) -> Unit,
    birthDate: Long,
    onSetBirthDate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMITSelector by remember { mutableStateOf(false) }
    var showQuadrantTasksDialog by remember { mutableStateOf<Quadrant?>(null) }
    val reminder = remember(state.quadrantCounts) { generateReminder(state.quadrantCounts) }
    val displayMIT = state.currentMIT ?: state.suggestedMIT

    // 提醒弹窗状态
    var reminderDialogTask by remember { mutableStateOf<TaskUiModel?>(null) }

    // 动态倒计时：每秒刷新一次
    var currentSecond by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    
    // 记录已播放提示音的任务ID（避免重复播放）
    var notifiedTaskIds by remember { mutableStateOf(setOf<Long>()) }
    
    // DatePicker Logic
    val calendar = Calendar.getInstance()
    if (birthDate > 0) {
        calendar.timeInMillis = birthDate
    }
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance()
            newCal.set(year, month, dayOfMonth)
            onSetBirthDate(newCal.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000) // 每秒刷新一次
            currentSecond = System.currentTimeMillis() / 1000
        }
    }

    // 根据当前时间过滤需要提醒的任务
    val activeReminders = remember(tasksWithReminder, currentSecond) {
        tasksWithReminder
            .filter { it.shouldShowReminder() }
            .sortedBy { it.getEffectiveDueTimestamp() ?: Long.MAX_VALUE }
    }
    
    // 检查是否有新任务需要提醒，播放提示音并弹出对话框
    LaunchedEffect(activeReminders) {
        val currentIds = activeReminders.map { it.id }.toSet()
        val newReminders = currentIds - notifiedTaskIds
        
        if (newReminders.isNotEmpty()) {
            // 有新的提醒任务，播放提示音
            playNotificationSound(context)
            // 更新已通知的任务ID
            notifiedTaskIds = notifiedTaskIds + newReminders
            
            // 弹出第一个新提醒任务的对话框
            val firstNewTask = activeReminders.find { it.id in newReminders }
            if (firstNewTask != null && reminderDialogTask == null) {
                reminderDialogTask = firstNewTask
            }
        }
        
        // 清理已不在提醒列表中的任务ID（任务完成或被删除）
        notifiedTaskIds = notifiedTaskIds.intersect(currentIds)
    }

    // 提醒弹窗
    reminderDialogTask?.let { task ->
        ReminderAlertDialog(
            task = task,
            onComplete = {
                onCompleteTask(task.id)
                reminderDialogTask = null
            },
            onSnooze = {
                // 简单关闭，下次刷新时如果还在提醒时间内会再次显示
                reminderDialogTask = null
            },
            onViewDetails = {
                onEditTask(task.id)
                reminderDialogTask = null
            },
            onDismiss = {
                reminderDialogTask = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = MITGradientStart,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "今日决策",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "每天只做对的事，而不是做更多的事",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${state.quadrantCounts.total}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MITGradientStart
                )
                Text(
                    text = "待办任务",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // MIT Card（传入 currentSecond 触发动态刷新倒计时）
        MITCard(
            displayMIT = displayMIT,
            isConfirmed = state.currentMIT != null,
            currentTimeMillis = currentSecond * 1000,
            onConfirmMIT = { displayMIT?.let { onSetMIT(it.id) } },
            onChangeMIT = { showMITSelector = true }
        )

        // Life Countdown Card
        LifeCountdownCard(
            birthDate = birthDate,
            onClick = { datePickerDialog.show() },
            modifier = Modifier.fillMaxWidth()
        )

        // Quadrant Distribution - 2x2 布局，可点击

        // Quadrant Distribution - 2x2 布局，可点击
        QuadrantMiniMap(
            counts = state.quadrantCounts,
            onQuadrantClick = { quadrant -> showQuadrantTasksDialog = quadrant },
            modifier = Modifier.fillMaxWidth()
        )

        // System Reminder
        SystemReminderCard(
            reminder = reminder,
            modifier = Modifier.fillMaxWidth()
        )

        // Task Reminder Area（任务提醒区域，传入 currentTimeMillis 触发动态刷新）
        if (activeReminders.isNotEmpty()) {
            TaskReminderArea(
                tasks = activeReminders,
                currentTimeMillis = currentSecond * 1000,
                onTaskClick = onEditTask,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // MIT Selector Dialog
    if (showMITSelector) {
        MITSelectorDialog(
            tasks = state.importantTasks,
            currentMIT = state.currentMIT,
            onSelect = { taskId ->
                onSetMIT(taskId)
                showMITSelector = false
            },
            onClear = {
                onClearMIT()
                showMITSelector = false
            },
            onDismiss = { showMITSelector = false }
        )
    }

    // Quadrant Tasks Dialog
    showQuadrantTasksDialog?.let { quadrant ->
        val tasks = when (quadrant) {
            Quadrant.IMPORTANT_URGENT -> state.q1Tasks
            Quadrant.IMPORTANT_NOT_URGENT -> state.q2Tasks
            Quadrant.URGENT_NOT_IMPORTANT -> state.q3Tasks
            Quadrant.NOT_IMPORTANT_NOT_URGENT -> state.q4Tasks
        }
        val quadrantInfo = getQuadrantInfo(quadrant)

        QuadrantTasksDialog(
            quadrant = quadrant,
            tasks = tasks,
            quadrantInfo = quadrantInfo,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onSetTaskStatus = onSetTaskStatus,
            onCompleteTask = onCompleteTask,
            onDismiss = { showQuadrantTasksDialog = null }
        )
    }
}

private data class QuadrantInfo(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color
)

private fun getQuadrantInfo(quadrant: Quadrant): QuadrantInfo {
    return when (quadrant) {
        Quadrant.IMPORTANT_URGENT -> QuadrantInfo("重要紧急", QuadrantRedLight, QuadrantRed)
        Quadrant.IMPORTANT_NOT_URGENT -> QuadrantInfo("重要不紧急", QuadrantBlueLight, QuadrantBlue)
        Quadrant.URGENT_NOT_IMPORTANT -> QuadrantInfo("紧急不重要", QuadrantOrangeLight, QuadrantOrange)
        Quadrant.NOT_IMPORTANT_NOT_URGENT -> QuadrantInfo("不重要不紧急", QuadrantGrayLight, QuadrantGray)
    }
}

/**
 * 计算剩余时间文本（支持动态倒计时，精确到秒）
 * @param dueTimestamp 截止时间戳（毫秒）
 * @param currentTimeMillis 当前时间（毫秒），传入此参数以触发重组实现动态刷新
 */
private fun formatRemainingTime(dueTimestamp: Long?, currentTimeMillis: Long = System.currentTimeMillis()): RemainingTimeInfo? {
    if (dueTimestamp == null) return null
    
    val now = currentTimeMillis
    val diff = dueTimestamp - now
    
    if (diff < 0) {
        // 已超期
        val absDiff = -diff
        val totalSeconds = absDiff / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours >= 24 -> {
                val days = hours / 24
                RemainingTimeInfo("超期", "${days}天", true)
            }
            hours > 0 -> RemainingTimeInfo("超期", "${hours}h${minutes}m", true)
            minutes > 0 -> RemainingTimeInfo("超期", "${minutes}m${seconds}s", true)
            else -> RemainingTimeInfo("超期", "${seconds}s", true)
        }
    } else {
        val totalSeconds = diff / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours >= 24 -> {
                val days = hours / 24
                val remainingHours = hours % 24
                if (remainingHours > 0) {
                    RemainingTimeInfo("${days}天", "${remainingHours}h", false)
                } else {
                    RemainingTimeInfo("${days}天", "剩余", false)
                }
            }
            hours > 0 -> RemainingTimeInfo("${hours}h", "${minutes}m", false)
            minutes > 0 -> RemainingTimeInfo("${minutes}m", "${seconds}s", false)
            else -> RemainingTimeInfo("${seconds}", "秒", false)
        }
    }
}

private data class RemainingTimeInfo(
    val primary: String,
    val secondary: String,
    val isOverdue: Boolean
)

@Composable
private fun MITCard(
    displayMIT: TaskUiModel?,
    isConfirmed: Boolean,
    currentTimeMillis: Long,
    onConfirmMIT: () -> Unit,
    onChangeMIT: () -> Unit
) {
    // 使用 currentTimeMillis 计算剩余时间，实现动态倒计时
    val remainingTime = displayMIT?.getEffectiveDueTimestamp()?.let { formatRemainingTime(it, currentTimeMillis) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MITGradientStart, MITGradientEnd)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            // 右上角剩余时间圆圈
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        color = if (remainingTime?.isOverdue == true) 
                            Color(0xFFFF5252).copy(alpha = 0.3f)
                        else 
                            Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (remainingTime != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = remainingTime.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingTime.isOverdue) Color(0xFFFF8A80) else Color.White
                        )
                        Text(
                            text = remainingTime.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (remainingTime.isOverdue) 
                                Color(0xFFFF8A80).copy(alpha = 0.8f) 
                            else 
                                Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "今日最重要的一件事",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (displayMIT != null) {
                    Text(
                        text = displayMIT.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 70.dp) // 为右上角圆圈留出空间
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isConfirmed) {
                            Surface(
                                onClick = onConfirmMIT,
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "确认为 MIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFD54F).copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFFE082),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "已设为 MIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFE082)
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onChangeMIT,
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "更换",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "暂无最重要的任务",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = onChangeMIT,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "选择今日 MIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantMiniMap(
    counts: QuadrantCounts,
    onQuadrantClick: (Quadrant) -> Unit,
    modifier: Modifier = Modifier
) {
    // 直接显示四象限卡片，无外层Card包裹，更简洁
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 第一行：重要紧急 | 重要不紧急
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuadrantClickableCell(
                count = counts.urgentImportant,
                label = "重要紧急",
                emoji = "🔥",
                backgroundColor = QuadrantRedLight,
                textColor = QuadrantRed,
                borderColor = QuadrantRed.copy(alpha = 0.3f),
                showWarning = counts.urgentImportant > 5,
                onClick = { onQuadrantClick(Quadrant.IMPORTANT_URGENT) },
                modifier = Modifier.weight(1f)
            )
            QuadrantClickableCell(
                count = counts.importantNotUrgent,
                label = "重要不紧急",
                emoji = "⭐",
                backgroundColor = QuadrantBlueLight,
                textColor = QuadrantBlue,
                borderColor = QuadrantBlue.copy(alpha = 0.3f),
                onClick = { onQuadrantClick(Quadrant.IMPORTANT_NOT_URGENT) },
                modifier = Modifier.weight(1f)
            )
        }

        // 第二行：紧急不重要 | 不重要不紧急
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuadrantClickableCell(
                count = counts.urgentNotImportant,
                label = "紧急不重要",
                emoji = "⚡",
                backgroundColor = QuadrantOrangeLight,
                textColor = QuadrantOrange,
                borderColor = QuadrantOrange.copy(alpha = 0.3f),
                onClick = { onQuadrantClick(Quadrant.URGENT_NOT_IMPORTANT) },
                modifier = Modifier.weight(1f)
            )
            QuadrantClickableCell(
                count = counts.notUrgentNotImportant,
                label = "不重要不紧急",
                emoji = "💤",
                backgroundColor = QuadrantGrayLight,
                textColor = QuadrantGray,
                borderColor = QuadrantGray.copy(alpha = 0.3f),
                onClick = { onQuadrantClick(Quadrant.NOT_IMPORTANT_NOT_URGENT) },
                modifier = Modifier.weight(1f)
            )
        }

        // 进度条
        if (counts.total > 0) {
            QuadrantProgressBar(counts = counts)
        }
    }
}

@Composable
private fun QuadrantClickableCell(
    count: Int,
    label: String,
    emoji: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
    showWarning: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 2.dp,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        // ... (existing content of QuadrantClickableCell, effectively keeping it but I need to close the function)
        // Wait, I am viewing lines 1-800, and the file ends around there? No, I saw "The above content does NOT show the entire file contents."
        // I need to append LifeCountdownCard at the end of the file.
        // It's risky to replace the end if I don't see it.
        // I'll assume I can just use a large line number or append.
        // But `multi_replace_file_content` requires exact match.
        // I will READ the end of the file first to be safe.
        // Cancel this specific replacement chunk for now and do it in a separate call or specific `view_file` first?
        // Actually, I'll separate the Append task.
        // I will only apply the first 3 chunks now.

        Box(modifier = Modifier.padding(14.dp)) {
            // 右上角警告点
            if (showWarning) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(QuadrantRed, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧：emoji + 标签
                Column {
                    Text(
                        text = emoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 右侧：数量
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

// 兼容旧接口
@Composable
private fun QuadrantClickableCellCompat(
    count: Int,
    label: String,
    backgroundColor: Color,
    textColor: Color,
    showWarning: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showWarning) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(QuadrantRed, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun QuadrantProgressBar(counts: QuadrantCounts) {
    val total = counts.total.toFloat().coerceAtLeast(1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (counts.urgentImportant > 0) {
            val fraction by animateFloatAsState(
                targetValue = counts.urgentImportant / total,
                animationSpec = tween(500),
                label = "q1"
            )
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxSize()
                    .background(QuadrantRed)
            )
        }
        if (counts.importantNotUrgent > 0) {
            val fraction by animateFloatAsState(
                targetValue = counts.importantNotUrgent / total,
                animationSpec = tween(500),
                label = "q2"
            )
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxSize()
                    .background(QuadrantBlue)
            )
        }
        if (counts.urgentNotImportant > 0) {
            val fraction by animateFloatAsState(
                targetValue = counts.urgentNotImportant / total,
                animationSpec = tween(500),
                label = "q3"
            )
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxSize()
                    .background(QuadrantOrange)
            )
        }
        if (counts.notUrgentNotImportant > 0) {
            val fraction by animateFloatAsState(
                targetValue = counts.notUrgentNotImportant / total,
                animationSpec = tween(500),
                label = "q4"
            )
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxSize()
                    .background(QuadrantGray)
            )
        }
    }
}

@Composable
private fun SystemReminderCard(
    reminder: ReminderType,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = reminder.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(reminder.iconBackgroundColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = reminder.icon,
                    contentDescription = null,
                    tint = reminder.contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = reminder.contentColor
                )
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = reminder.contentColor.copy(alpha = 0.85f),
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 任务提醒区域
 */
@Composable
private fun TaskReminderArea(
    tasks: List<TaskUiModel>,
    currentTimeMillis: Long,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val ReminderYellow = Color(0xFFFFA000)
    val ReminderYellowLight = Color(0xFFFFF8E1)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ReminderYellowLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ReminderYellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = ReminderYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "任务提醒",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ReminderYellow.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${tasks.size} 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = ReminderYellow,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 提醒任务列表（传入 currentTimeMillis 实现动态倒计时）
            tasks.take(3).forEach { task ->
                TaskReminderItem(
                    task = task,
                    currentTimeMillis = currentTimeMillis,
                    onClick = { onTaskClick(task.id) }
                )
            }

            // 如果有更多任务
            if (tasks.size > 3) {
                Text(
                    text = "还有 ${tasks.size - 3} 项任务即将到期...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100).copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskReminderItem(
    task: TaskUiModel,
    currentTimeMillis: Long,
    onClick: () -> Unit
) {
    val ReminderYellow = Color(0xFFFFA000)
    val OverdueRed = Color(0xFFE53935)
    // 使用 currentTimeMillis 判断是否过期，实现动态更新
    val dueTime = task.getEffectiveDueTimestamp()
    val isOverdue = dueTime != null && currentTimeMillis > dueTime
    
    val quadrantColor = when (task.quadrant) {
        Quadrant.IMPORTANT_URGENT -> QuadrantRed
        Quadrant.IMPORTANT_NOT_URGENT -> QuadrantBlue
        Quadrant.URGENT_NOT_IMPORTANT -> QuadrantOrange
        Quadrant.NOT_IMPORTANT_NOT_URGENT -> QuadrantGray
    }

    // 计算距离截止时间（使用 currentTimeMillis 实现动态倒计时）
    val remainingTime = task.getEffectiveDueTimestamp()?.let { formatRemainingTime(it, currentTimeMillis) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isOverdue) 
            OverdueRed.copy(alpha = 0.08f) 
        else 
            Color.White.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 象限颜色指示器（过期时显示红色）
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isOverdue) OverdueRed else quadrantColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isOverdue) OverdueRed else MaterialTheme.colorScheme.onSurface
                    )
                    // 过期或提醒信息
                    if (isOverdue) {
                        Text(
                            text = "⚠ 已过期，请尽快处理",
                            style = MaterialTheme.typography.labelSmall,
                            color = OverdueRed
                        )
                    } else {
                        val reminderDesc = task.getReminderDescription()
                        if (reminderDesc.isNotEmpty()) {
                            Text(
                                text = reminderDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 状态标签
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isOverdue) 
                    OverdueRed.copy(alpha = 0.15f)
                else if (remainingTime != null)
                    ReminderYellow.copy(alpha = 0.15f)
                else
                    Color.Transparent
            ) {
                Text(
                    text = if (isOverdue) 
                        "已过期" 
                    else if (remainingTime != null)
                        "剩余 ${remainingTime.primary}${remainingTime.secondary}"
                    else
                        "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue) OverdueRed else ReminderYellow,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * 象限任务列表弹窗
 */
@Composable
private fun QuadrantTasksDialog(
    quadrant: Quadrant,
    tasks: List<TaskUiModel>,
    quadrantInfo: QuadrantInfo,
    onEditTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onSetTaskStatus: (Long, TaskStatus) -> Unit,
    onCompleteTask: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(quadrantInfo.backgroundColor)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = quadrantInfo.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = quadrantInfo.textColor
                        )
                        Text(
                            text = "共 ${tasks.size} 项任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = quadrantInfo.textColor.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = quadrantInfo.textColor
                        )
                    }
                }

                // Task list
                if (tasks.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskListItem(
                                task = task,
                                quadrantInfo = quadrantInfo,
                                onEdit = { onEditTask(task.id) },
                                onDelete = { onDeleteTask(task.id) },
                                onSetStatus = { status -> onSetTaskStatus(task.id, status) },
                                onComplete = { onCompleteTask(task.id) },
                                onDismissDialog = onDismiss
                            )
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    quadrantInfo.backgroundColor,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = quadrantInfo.textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无任务",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "该象限当前没有待办任务",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Footer
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "关闭",
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(
    task: TaskUiModel,
    quadrantInfo: QuadrantInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetStatus: (TaskStatus) -> Unit,
    onComplete: () -> Unit,
    onDismissDialog: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isCompleted = task.status == TaskStatus.COMPLETED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 完成按钮
            Surface(
                onClick = { if (!isCompleted) onComplete() },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = if (isCompleted) quadrantInfo.textColor else quadrantInfo.textColor.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .background(
                            if (isCompleted) quadrantInfo.textColor else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "已完成",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 任务内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                // 状态标签
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusText = when (task.status) {
                        TaskStatus.IN_PROGRESS -> "进行中"
                        TaskStatus.COMPLETED -> "已完成"
                        TaskStatus.OVERDUE -> "已过期"
                    }
                    // 状态颜色定义：进行中-蓝色, 已完成-绿色, 已过期-红色
                    val statusColor = when (task.status) {
                        TaskStatus.IN_PROGRESS -> Color(0xFF1E88E5)  // 蓝色
                        TaskStatus.COMPLETED -> Color(0xFF43A047)    // 绿色
                        TaskStatus.OVERDUE -> Color(0xFFE53935)      // 红色
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    
                    if (task.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFB300).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "置顶",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB300),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 更多操作按钮
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onDismissDialog()
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    
                    if (!isCompleted) {
                        DropdownMenuItem(
                            text = { Text("标记为进行中") },
                            onClick = {
                                showMenu = false
                                onSetStatus(TaskStatus.IN_PROGRESS)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("标记为完成") },
                            onClick = {
                                showMenu = false
                                onComplete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("重新开始") },
                            onClick = {
                                showMenu = false
                                onSetStatus(TaskStatus.IN_PROGRESS)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("删除", color = Color(0xFFE53935)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFE53935)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MITSelectorDialog(
    tasks: List<TaskUiModel>,
    currentMIT: TaskUiModel?,
    onSelect: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "选择今日最重要的任务",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "从重要任务中选择一项作为今天的首要目标",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }

                if (tasks.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            MITTaskItem(
                                task = task,
                                isCurrentMIT = task.id == currentMIT?.id,
                                onClick = { onSelect(task.id) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无重要任务",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "先添加一些重要任务吧",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentMIT != null) {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清除 MIT")
                        }
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "关闭",
                            modifier = Modifier.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MITTaskItem(
    task: TaskUiModel,
    isCurrentMIT: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isCurrentMIT) MITGradientStart else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val backgroundColor = if (isCurrentMIT) MITGradientStart.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = when (task.quadrant) {
                            Quadrant.IMPORTANT_URGENT -> QuadrantRedLight
                            Quadrant.IMPORTANT_NOT_URGENT -> QuadrantBlueLight
                            else -> QuadrantGrayLight
                        },
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentMIT) Icons.Default.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = when (task.quadrant) {
                        Quadrant.IMPORTANT_URGENT -> QuadrantRed
                        Quadrant.IMPORTANT_NOT_URGENT -> QuadrantBlue
                        else -> QuadrantGray
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (task.quadrant) {
                        Quadrant.IMPORTANT_URGENT -> "重要紧急"
                        Quadrant.IMPORTANT_NOT_URGENT -> "重要不紧急"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isCurrentMIT) {
                Text(
                    text = "当前 MIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MITGradientStart,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LifeCountdownCard(
    birthDate: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifeExpectancyYears = 80
    
    val ageInfo = remember(birthDate) {
        if (birthDate == 0L) return@remember null
        
        val birthCalendar = Calendar.getInstance().apply { timeInMillis = birthDate }
        val now = Calendar.getInstance()
        
        var age = now.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        
        val totalDays = (lifeExpectancyYears * 365.25).toLong()
        val livedDays = ((now.timeInMillis - birthDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        val remainingDays = (totalDays - livedDays).coerceAtLeast(0)
        val progress = (livedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
        
        Triple(age, remainingDays, progress)
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF263238), // Dark Blue Grey
                            Color(0xFF37474F)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⏳", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "生命倒计时 (向死而生)",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (ageInfo != null) {
                         Text(
                            text = "${(ageInfo.third * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF80CBC4),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (ageInfo == null) {
                    Text(
                        text = "点击设置出生日期，开启生命倒计时",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    val (age, remainingDays, progress) = ageInfo
                    val remainingYears = remainingDays / 365
                    val remainingDaysLeft = remainingDays % 365
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "已使用",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "$age 年",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "剩余约 (假设80岁)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "$remainingYears 年 $remainingDaysLeft 天",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF80CBC4), // Teal 200
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = when {
                            progress > 0.8f -> Color(0xFFFF5252) // Red
                            progress > 0.5f -> Color(0xFFFFD54F) // Yellow
                            else -> Color(0xFF69F0AE) // Green
                        },
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                }
            }
        }
    }
}
