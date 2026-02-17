package com.xltool.quadrant.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskRepository
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.TaskUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// 颜色定义
private val QuadrantRed = Color(0xFFE53935)
private val QuadrantBlue = Color(0xFF1E88E5)
private val QuadrantOrange = Color(0xFFFB8C00)
private val QuadrantGray = Color(0xFF757575)
private val PrimaryPurple = Color(0xFF5C6BC0)

enum class SearchMode {
    DAY, WEEK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSearchScreen(
    repository: TaskRepository,
    onNavigateBack: () -> Unit,
    onEditTask: (Long) -> Unit
) {
    var searchMode by remember { mutableStateOf(SearchMode.WEEK) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 计算周的起始和结束日期
    val weekStart = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val weekEnd = remember(weekStart) {
        weekStart.plusDays(6)
    }

    // 根据查询模式获取任务
    val tasksFlow: Flow<List<TaskUiModel>> = remember(searchMode, selectedDate, weekStart, weekEnd) {
        when (searchMode) {
            SearchMode.DAY -> repository.observeTasksByDateRange(selectedDate, selectedDate)
            SearchMode.WEEK -> repository.observeTasksByDateRange(weekStart, weekEnd)
        }
    }

    val tasks by tasksFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "任务查询",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 查询模式切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = searchMode == SearchMode.WEEK,
                    onClick = { searchMode = SearchMode.WEEK },
                    label = { Text("按周查询") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryPurple,
                        selectedLeadingIconColor = PrimaryPurple
                    )
                )
                FilterChip(
                    selected = searchMode == SearchMode.DAY,
                    onClick = { searchMode = SearchMode.DAY },
                    label = { Text("按日查询") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryPurple,
                        selectedLeadingIconColor = PrimaryPurple
                    )
                )
            }

            // 日期选择器
            DateSelector(
                searchMode = searchMode,
                selectedDate = selectedDate,
                weekStart = weekStart,
                weekEnd = weekEnd,
                onPrevious = {
                    selectedDate = when (searchMode) {
                        SearchMode.DAY -> selectedDate.minusDays(1)
                        SearchMode.WEEK -> selectedDate.minusWeeks(1)
                    }
                },
                onNext = {
                    selectedDate = when (searchMode) {
                        SearchMode.DAY -> selectedDate.plusDays(1)
                        SearchMode.WEEK -> selectedDate.plusWeeks(1)
                    }
                },
                onSelectDate = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 任务统计
            TaskStatistics(
                tasks = tasks,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 任务列表
            if (tasks.isEmpty()) {
                EmptyState(
                    searchMode = searchMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    items(tasks, key = { it.id }) { task ->
                        SearchTaskItem(
                            task = task,
                            onClick = { onEditTask(task.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // 日期选择对话框
    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateSelector(
    searchMode: SearchMode,
    selectedDate: LocalDate,
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    val weekFormatter = DateTimeFormatter.ofPattern("M月d日")

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryPurple.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "上一${if (searchMode == SearchMode.DAY) "天" else "周"}",
                    tint = PrimaryPurple
                )
            }

            Surface(
                onClick = onSelectDate,
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (searchMode) {
                            SearchMode.DAY -> selectedDate.format(dateFormatter)
                            SearchMode.WEEK -> "${weekStart.format(weekFormatter)} - ${weekEnd.format(weekFormatter)}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryPurple
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "下一${if (searchMode == SearchMode.DAY) "天" else "周"}",
                    tint = PrimaryPurple
                )
            }
        }
    }
}

@Composable
private fun TaskStatistics(
    tasks: List<TaskUiModel>,
    modifier: Modifier = Modifier
) {
    val completed = tasks.count { it.status == TaskStatus.COMPLETED }
    val inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS }
    val overdue = tasks.count { it.status == TaskStatus.OVERDUE }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatisticChip(
            label = "总计",
            count = tasks.size,
            color = PrimaryPurple,
            modifier = Modifier.weight(1f)
        )
        StatisticChip(
            label = "已完成",
            count = completed,
            color = Color(0xFF43A047),
            modifier = Modifier.weight(1f)
        )
        StatisticChip(
            label = "进行中",
            count = inProgress,
            color = Color(0xFF1E88E5),
            modifier = Modifier.weight(1f)
        )
        StatisticChip(
            label = "已过期",
            count = overdue,
            color = Color(0xFFE53935),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatisticChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SearchTaskItem(
    task: TaskUiModel,
    onClick: () -> Unit
) {
    val quadrantColor = when (task.quadrant) {
        Quadrant.IMPORTANT_URGENT -> QuadrantRed
        Quadrant.IMPORTANT_NOT_URGENT -> QuadrantBlue
        Quadrant.URGENT_NOT_IMPORTANT -> QuadrantOrange
        Quadrant.NOT_IMPORTANT_NOT_URGENT -> QuadrantGray
    }

    val statusColor = when (task.status) {
        TaskStatus.COMPLETED -> Color(0xFF43A047)
        TaskStatus.IN_PROGRESS -> Color(0xFF1E88E5)
        TaskStatus.OVERDUE -> Color(0xFFE53935)
    }

    val isCompleted = task.status == TaskStatus.COMPLETED

    // 格式化截止日期
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    val dueText = task.dueTimestamp?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(dateFormatter)
    } ?: task.dueEpochDay?.let {
        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("MM/dd"))
    }

    Card(
        onClick = onClick,
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
            // 象限颜色指示条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(quadrantColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 任务信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 象限标签
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = quadrantColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = task.quadrant.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = quadrantColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // 状态标签
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = task.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // 截止日期
                    if (dueText != null) {
                        Text(
                            text = "截止: $dueText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    searchMode: SearchMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    PrimaryPurple.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = PrimaryPurple.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无任务",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (searchMode == SearchMode.DAY) "该日期没有任务安排" else "该周没有任务安排",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

