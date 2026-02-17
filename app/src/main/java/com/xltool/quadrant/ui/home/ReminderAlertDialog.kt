package com.xltool.quadrant.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskUiModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 颜色定义
private val AlertGradientStart = Color(0xFFFF6B6B)
private val AlertGradientEnd = Color(0xFFEE5A5A)
private val AlertBg = Color(0xFFFFF5F5)

private val QuadrantRed = Color(0xFFE53935)
private val QuadrantBlue = Color(0xFF1E88E5)
private val QuadrantOrange = Color(0xFFFB8C00)
private val QuadrantGray = Color(0xFF757575)

/**
 * 任务提醒弹窗
 * 当任务到达提醒时间时显示
 */
@Composable
fun ReminderAlertDialog(
    task: TaskUiModel,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    // 铃铛动画
    val infiniteTransition = rememberInfiniteTransition(label = "bell")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell_scale"
    )

    val quadrantColor = when (task.quadrant) {
        Quadrant.IMPORTANT_URGENT -> QuadrantRed
        Quadrant.IMPORTANT_NOT_URGENT -> QuadrantBlue
        Quadrant.URGENT_NOT_IMPORTANT -> QuadrantOrange
        Quadrant.NOT_IMPORTANT_NOT_URGENT -> QuadrantGray
    }

    // 格式化到期时间
    val dueTimeText = task.dueTimestamp?.let { timestamp ->
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        dateTime.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
    } ?: task.dueEpochDay?.let { epochDay ->
        java.time.LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("MM月dd日"))
    } ?: "未设置"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部渐变背景 + 铃铛图标
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(AlertGradientStart, AlertGradientEnd)
                            ),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 铃铛图标（带动画）
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .scale(scale)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "任务提醒",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 任务信息
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 任务标题
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 象限和时间信息
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 象限标签
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = quadrantColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(quadrantColor, CircleShape)
                                )
                                Text(
                                    text = task.quadrant.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = quadrantColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 到期时间
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AlertGradientStart.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = AlertGradientStart,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = dueTimeText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AlertGradientStart,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 描述（如果有）
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 稍后提醒
                        OutlinedButton(
                            onClick = onSnooze,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("稍后")
                        }

                        // 标记完成
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF43A047)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("完成")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 查看详情
                    TextButton(
                        onClick = {
                            onDismiss()
                            onViewDetails()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "查看详情",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
