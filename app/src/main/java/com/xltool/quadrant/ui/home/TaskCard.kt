package com.xltool.quadrant.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xltool.quadrant.R
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.TaskUiModel
import com.xltool.quadrant.util.formatEpochDayShortCn
import java.time.LocalDate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

@Composable
fun TaskCard(
    task: TaskUiModel,
    index: Int,
    total: Int,
    isAnyDragging: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onDeferOneDay: () -> Unit,
    onMoveTo: (Quadrant) -> Unit,
    onTogglePinned: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetStatus: (TaskStatus) -> Unit,
    onDragStart: (TaskUiModel, Rect, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var boundsInWindow by remember { mutableStateOf<Rect?>(null) }
    val scope = rememberCoroutineScope()

    val statusColor = statusAccent(task.status)
    val dueText = task.dueEpochDay?.let(::formatEpochDayShortCn)
    val isOverdue = task.dueEpochDay != null && task.dueEpochDay < LocalDate.now().toEpochDay()

    val density = LocalDensity.current
    val startActionsWidth = 96.dp
    val endActionsWidth = 192.dp // 两个操作：完成/删除
    val startMaxPx = with(density) { startActionsWidth.toPx() }
    val endMaxPx = with(density) { endActionsWidth.toPx() }
    val offsetX = remember(task.id) { Animatable(0f) }
    val swipeOpen = abs(offsetX.value) > 1f

    Box(modifier = Modifier.fillMaxWidth()) {
        // 背景操作区
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // 右滑（Start）: 推迟一天
            Box(
                modifier = Modifier
                    .width(startActionsWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1565C0)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Filled.Schedule, contentDescription = null, tint = Color.White)
                    Text(text = stringResource(R.string.action_defer_one_day), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = offsetX.value > 24f) {
                            onDeferOneDay()
                            scope.launch { offsetX.animateTo(0f, tween(160)) }
                        }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 左滑（End）: 完成 / 删除
            Row(
                modifier = Modifier
                    .width(endActionsWidth)
                    .fillMaxHeight()
            ) {
                SwipeAction(
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFF2E7D32),
                    icon = Icons.Filled.Check,
                    label = stringResource(R.string.action_complete),
                    enabled = offsetX.value < -24f,
                    onClick = {
                        onComplete()
                        scope.launch { offsetX.animateTo(0f, tween(160)) }
                    }
                )
                SwipeAction(
                    modifier = Modifier.weight(1f),
                    bg = Color(0xFFC62828),
                    icon = Icons.Filled.Delete,
                    label = stringResource(R.string.action_delete),
                    enabled = offsetX.value < -24f,
                    onClick = {
                        onDelete()
                        scope.launch { offsetX.animateTo(0f, tween(160)) }
                    }
                )
            }
        }

        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .alpha(if (isDragging) 0.15f else 1f)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    boundsInWindow = coordinates.boundsInWindow()
                }
                .pointerInput(task.id, isAnyDragging) {
                    if (isAnyDragging) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(-endMaxPx, startMaxPx)
                                offsetX.snapTo(next)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val target = when {
                                    offsetX.value > startMaxPx * 0.50f -> startMaxPx
                                    offsetX.value < -endMaxPx * 0.50f -> -endMaxPx
                                    else -> 0f
                                }
                                offsetX.animateTo(target, tween(160))
                            }
                        }
                    )
                }
                .pointerInput(task.id, isAnyDragging, swipeOpen) {
                    if (isAnyDragging || swipeOpen) return@pointerInput
                    detectDragGesturesAfterLongPress(
                        onDragStart = { startOffset ->
                            val rect = boundsInWindow ?: return@detectDragGesturesAfterLongPress
                            onDragStart(task, rect, startOffset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                },
            onClick = {
                if (isAnyDragging) return@Card
                if (swipeOpen) {
                    scope.launch { offsetX.animateTo(0f, tween(160)) }
                } else {
                    onClick()
                }
            },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(statusColor.copy(alpha = 0.85f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (!dueText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dueText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        if (task.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = stringResource(R.string.action_pin),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.action_more),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (task.isPinned) stringResource(R.string.action_unpin)
                                        else stringResource(R.string.action_pin)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePinned()
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_sort), style = MaterialTheme.typography.labelMedium) },
                                onClick = { },
                                enabled = false
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_move_up)) },
                                onClick = {
                                    menuExpanded = false
                                    onMoveUp()
                                },
                                enabled = index > 0
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_move_down)) },
                                onClick = {
                                    menuExpanded = false
                                    onMoveDown()
                                },
                                enabled = index < total - 1
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_move_to), style = MaterialTheme.typography.labelMedium) },
                                onClick = { },
                                enabled = false
                            )
                            Quadrant.entries.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q.displayName) },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveTo(q)
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_status), style = MaterialTheme.typography.labelMedium) },
                                onClick = { },
                                enabled = false
                            )
                            listOf(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED).forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.displayName) },
                                    onClick = {
                                        menuExpanded = false
                                        onSetStatus(status)
                                    },
                                    leadingIcon = {
                                        if (status == TaskStatus.COMPLETED) {
                                            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = {
                                    menuExpanded = false
                                    confirmDelete = true
                                }
                            )
                        }
                    }

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val tags = task.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (tags.isNotEmpty() || task.status != TaskStatus.IN_PROGRESS) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.take(3).forEach { tag ->
                                TagPill(text = tag)
                            }
                            if (tags.size > 3) {
                                TagPill(text = "+${tags.size - 3}")
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text(
                                text = task.status.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, task.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun TagPill(text: String, height: Dp = 22.dp) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun statusAccent(status: TaskStatus): Color {
    return when (status) {
        TaskStatus.IN_PROGRESS -> Color(0xFF1E3A5F) // 使用主色调
        TaskStatus.COMPLETED -> Color(0xFF43A047)
        TaskStatus.OVERDUE -> Color(0xFFE53935)
    }
}

@Composable
private fun SwipeAction(
    modifier: Modifier,
    bg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}
