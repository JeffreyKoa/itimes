package com.xltool.quadrant.ui.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskRepository
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.TaskUiModel
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.temporal.TemporalAdjusters

/**
 * 统计数据
 */
data class ReviewStats(
    val totalTasks: Int = 0,
    // 三种状态统计
    val inProgressCount: Int = 0,   // 进行中
    val completedCount: Int = 0,    // 已完成
    val overdueCount: Int = 0,      // 已过期
    // 重要任务统计
    val importantTasks: Int = 0,
    val importantCompleted: Int = 0,
    // 四象限统计
    val q1Count: Int = 0,
    val q2Count: Int = 0,
    val q3Count: Int = 0,
    val q4Count: Int = 0,
    val q1Completed: Int = 0,
    val q2Completed: Int = 0,
    val q3Completed: Int = 0,
    val q4Completed: Int = 0,
    // 标签统计
    val tagCounts: Map<String, Int> = emptyMap()
) {
    val completionRate: Float
        get() = if (totalTasks > 0) completedCount.toFloat() / totalTasks else 0f
    
    val importantCompletionRate: Float
        get() = if (importantTasks > 0) importantCompleted.toFloat() / importantTasks else 0f
    
    val q1Rate: Float get() = if (q1Count > 0) q1Completed.toFloat() / q1Count else 0f
    val q2Rate: Float get() = if (q2Count > 0) q2Completed.toFloat() / q2Count else 0f
    val q3Rate: Float get() = if (q3Count > 0) q3Completed.toFloat() / q3Count else 0f
    val q4Rate: Float get() = if (q4Count > 0) q4Completed.toFloat() / q4Count else 0f
}

/**
 * 从任务列表计算统计数据
 */
private fun calculateStats(
    tasks: List<TaskUiModel>,
    startMillis: Long,
    endMillis: Long
): ReviewStats {
    // 过滤在时间范围内创建的任务
    val filteredTasks = tasks.filter { task ->
        task.createdAt in startMillis..endMillis
    }
    
    // 三种状态统计
    val inProgress = filteredTasks.count { it.status == TaskStatus.IN_PROGRESS }
    val completed = filteredTasks.count { it.status == TaskStatus.COMPLETED }
    val overdue = filteredTasks.count { it.status == TaskStatus.OVERDUE }
    
    // 重要任务统计
    val important = filteredTasks.filter { 
        it.quadrant == Quadrant.IMPORTANT_URGENT || it.quadrant == Quadrant.IMPORTANT_NOT_URGENT 
    }
    val importantCompleted = important.count { it.status == TaskStatus.COMPLETED }
    
    // 四象限统计
    val q1 = filteredTasks.filter { it.quadrant == Quadrant.IMPORTANT_URGENT }
    val q2 = filteredTasks.filter { it.quadrant == Quadrant.IMPORTANT_NOT_URGENT }
    val q3 = filteredTasks.filter { it.quadrant == Quadrant.URGENT_NOT_IMPORTANT }
    val q4 = filteredTasks.filter { it.quadrant == Quadrant.NOT_IMPORTANT_NOT_URGENT }
    
    // 标签统计
    val tagCounts = mutableMapOf<String, Int>()
    filteredTasks.forEach { task ->
        task.tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
    }
    
    return ReviewStats(
        totalTasks = filteredTasks.size,
        inProgressCount = inProgress,
        completedCount = completed,
        overdueCount = overdue,
        importantTasks = important.size,
        importantCompleted = importantCompleted,
        q1Count = q1.size,
        q2Count = q2.size,
        q3Count = q3.size,
        q4Count = q4.size,
        q1Completed = q1.count { it.status == TaskStatus.COMPLETED },
        q2Completed = q2.count { it.status == TaskStatus.COMPLETED },
        q3Completed = q3.count { it.status == TaskStatus.COMPLETED },
        q4Completed = q4.count { it.status == TaskStatus.COMPLETED },
        tagCounts = tagCounts
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    repository: TaskRepository? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("本周", "本月", "本年", "自定义")
    
    // 当前时间（每分钟刷新一次）
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }
    
    // 计算时间范围
    val now = LocalDateTime.now()
    val zone = ZoneId.systemDefault()
    
    // 自定义日期范围状态
    var customStartDate by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    val (startMillis, endMillis, periodLabel) = remember(selectedTab, currentTimeMillis, customStartDate, customEndDate) {
        when (selectedTab) {
            0 -> { // 本周（周一到现在）
                val monday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = currentTimeMillis
                val formatter = DateTimeFormatter.ofPattern("M月d日")
                Triple(start, end, "${monday.format(formatter)} - 现在")
            }
            1 -> { // 本月（1号到现在）
                val firstDay = now.toLocalDate().withDayOfMonth(1)
                val start = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = currentTimeMillis
                Triple(start, end, "${now.monthValue}月1日 - 现在")
            }
            2 -> { // 本年（1月1日到现在）
                val firstDay = LocalDate.of(now.year, 1, 1)
                val start = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = currentTimeMillis
                Triple(start, end, "${now.year}年1月1日 - 现在")
            }
            else -> { // 自定义日期范围
                val start = customStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = customEndDate.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
                Triple(start, end, "${customStartDate.format(dateFormatter)} - ${customEndDate.format(dateFormatter)}")
            }
        }
    }
    
    // 观察所有任务
    val allTasks = repository?.observeAllTasksForStats()
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // 计算统计数据
    val stats = remember(allTasks?.value, startMillis, endMillis) {
        allTasks?.value?.let { tasks ->
            calculateStats(tasks, startMillis, endMillis)
        } ?: ReviewStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("数据复盘")
                        Text(
                            text = "实时统计 · 截止到当前时间",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 时间维度选择
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.CalendarToday
                                    1 -> Icons.Default.CalendarMonth
                                    2 -> Icons.Default.DateRange
                                    else -> Icons.Default.Edit
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
            
            // 统计内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 时间范围显示
                Text(
                    text = "📅 统计周期：$periodLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 自定义日期选择器（仅在选择"自定义"Tab时显示）
                if (selectedTab == 3) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 开始日期
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { showStartDatePicker = true }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "开始日期",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = customStartDate.format(dateFormatter),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            // 结束日期
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { showEndDatePicker = true }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "结束日期",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = customEndDate.format(dateFormatter),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 核心数据卡片
                OverviewCard(stats = stats)
                
                // 状态分布卡片（五种状态）
                StatusDistributionCard(stats = stats)
                
                // 完成率卡片
                CompletionRateCard(stats = stats)
                
                // 象限分布卡片
                QuadrantDistributionCard(stats = stats)
                
                // 标签分布雷达图
                if (stats.tagCounts.isNotEmpty()) {
                    TagStatsCard(tagCounts = stats.tagCounts)
                }
                
                // 轻复盘问题
                LightReviewCard()
            }
        }
    }
    
    // 开始日期选择器对话框
    if (showStartDatePicker) {
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            customStartDate = Instant.ofEpochMilli(millis)
                                .atZone(zone)
                                .toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }
    
    // 结束日期选择器对话框
    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customEndDate.atStartOfDay(zone).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            customEndDate = Instant.ofEpochMilli(millis)
                                .atZone(zone)
                                .toLocalDate()
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@Composable
private fun OverviewCard(stats: ReviewStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "任务概览",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "数据实时更新中",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 统计数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = "${stats.totalTasks}",
                        label = "创建任务",
                        icon = Icons.Default.Timer
                    )
                    StatItem(
                        value = "${stats.completedCount}",
                        label = "已完成",
                        icon = Icons.Default.CheckCircle
                    )
                    StatItem(
                        value = "${(stats.completionRate * 100).toInt()}%",
                        label = "完成率",
                        icon = Icons.Default.TrendingUp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 状态分布卡片 - 显示三种状态的统计
 */
@Composable
private fun StatusDistributionCard(stats: ReviewStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📋 状态分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 三种状态：进行中、已完成、已过期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusStatItem(
                    count = stats.inProgressCount,
                    label = "进行中",
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.weight(1f)
                )
                StatusStatItem(
                    count = stats.completedCount,
                    label = "已完成",
                    color = Color(0xFF43A047),
                    modifier = Modifier.weight(1f)
                )
                StatusStatItem(
                    count = stats.overdueCount,
                    label = "已过期",
                    color = Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusStatItem(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    sublabel: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun CompletionRateCard(stats: ReviewStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📊 完成率分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 总完成率
            RateProgressItem(
                label = "总完成率",
                completed = stats.completedCount,
                total = stats.totalTasks,
                rate = stats.completionRate,
                color = Color(0xFF667eea)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 重要任务完成率
            RateProgressItem(
                label = "重要任务完成率",
                completed = stats.importantCompleted,
                total = stats.importantTasks,
                rate = stats.importantCompletionRate,
                color = Color(0xFFE53935)
            )
        }
    }
}

@Composable
private fun RateProgressItem(
    label: String,
    completed: Int,
    total: Int,
    rate: Float,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = rate,
        animationSpec = tween(1000),
        label = "progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$completed / $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(rate * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun QuadrantDistributionCard(stats: ReviewStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🎯 象限分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 2x2 网格
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuadrantStatCell(
                        label = "重要紧急",
                        emoji = "🔥",
                        count = stats.q1Count,
                        completed = stats.q1Completed,
                        color = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantStatCell(
                        label = "重要不紧急",
                        emoji = "⭐",
                        count = stats.q2Count,
                        completed = stats.q2Completed,
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuadrantStatCell(
                        label = "紧急不重要",
                        emoji = "⚡",
                        count = stats.q3Count,
                        completed = stats.q3Completed,
                        color = Color(0xFFFB8C00),
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantStatCell(
                        label = "不重要不紧急",
                        emoji = "💤",
                        count = stats.q4Count,
                        completed = stats.q4Completed,
                        color = Color(0xFF757575),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuadrantStatCell(
    label: String,
    emoji: String,
    count: Int,
    completed: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val rate = if (count > 0) completed.toFloat() / count else 0f
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "完成 $completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = color.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${(rate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun LightReviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "💭 轻复盘",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "回顾以下问题，帮助你更好地规划时间：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ReviewQuestion(number = 1, question = "哪件紧急的事其实不重要？")
            ReviewQuestion(number = 2, question = "哪件重要的事被我拖延了？")
            ReviewQuestion(number = 3, question = "下周我想少做什么？")
        }
    }
}

@Composable
private fun ReviewQuestion(number: Int, question: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
