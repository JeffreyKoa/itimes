package com.xltool.quadrant.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xltool.quadrant.R
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskStatus
import com.xltool.quadrant.data.TaskUiModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox

@Composable
fun QuadrantPanel(
    modifier: Modifier,
    quadrant: Quadrant,
    tasks: List<TaskUiModel>,
    isDropTargetHighlighted: Boolean,
    onBoundsInWindow: (Quadrant, Rect) -> Unit,
    isAnyDragging: Boolean,
    draggingTaskId: Long?,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onMoveTo: (Long, Quadrant) -> Unit,
    onTogglePinned: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onSetStatus: (Long, TaskStatus) -> Unit,
    onComplete: (Long) -> Unit,
    onDeferOneDay: (Long) -> Unit,
    onTaskDragStart: (TaskUiModel, Rect, Offset) -> Unit,
    onTaskDrag: (Offset) -> Unit,
    onTaskDragEnd: () -> Unit,
    onTaskDragCancel: () -> Unit
) {
    val accent = quadrantAccent(quadrant)
    Card(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                onBoundsInWindow(quadrant, coordinates.boundsInWindow())
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ,
        border = if (isDropTargetHighlighted) BorderStroke(2.dp, accent) else null
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quadrant.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.tasks_count, tasks.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.action_add_task)
                    )
                }
            }

            if (tasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quadrantEmptyText(quadrant),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tasks, key = { _, t -> t.id }) { index, task ->
                        TaskCard(
                            task = task,
                            index = index,
                            total = tasks.size,
                            isAnyDragging = isAnyDragging,
                            isDragging = draggingTaskId == task.id,
                            onClick = { onEdit(task.id) },
                            onDelete = { onDelete(task.id) },
                            onComplete = { onComplete(task.id) },
                            onDeferOneDay = { onDeferOneDay(task.id) },
                            onMoveTo = { q -> onMoveTo(task.id, q) },
                            onTogglePinned = { onTogglePinned(task.id) },
                            onMoveUp = { onMoveUp(task.id) },
                            onMoveDown = { onMoveDown(task.id) },
                            onSetStatus = { s -> onSetStatus(task.id, s) },
                            onDragStart = onTaskDragStart,
                            onDrag = onTaskDrag,
                            onDragEnd = onTaskDragEnd,
                            onDragCancel = onTaskDragCancel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun quadrantAccent(q: Quadrant): Color {
    return when (q) {
        Quadrant.IMPORTANT_URGENT -> Color(0xFFE53935)
        Quadrant.IMPORTANT_NOT_URGENT -> Color(0xFF43A047)
        Quadrant.URGENT_NOT_IMPORTANT -> Color(0xFFF9A825)
        Quadrant.NOT_IMPORTANT_NOT_URGENT -> Color(0xFF546E7A)
    }
}

@Composable
private fun quadrantEmptyText(q: Quadrant): String {
    return when (q) {
        Quadrant.IMPORTANT_URGENT -> stringResource(R.string.empty_q1)
        Quadrant.IMPORTANT_NOT_URGENT -> stringResource(R.string.empty_q2)
        Quadrant.URGENT_NOT_IMPORTANT -> stringResource(R.string.empty_q3)
        Quadrant.NOT_IMPORTANT_NOT_URGENT -> stringResource(R.string.empty_q4)
    }
}


