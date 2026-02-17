package com.xltool.quadrant.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 通用3D可拖动悬浮按钮
 * - 有立体3D效果（渐变、阴影、高光）
 * - 可拖动到屏幕任意位置
 * - 点击时有缩放反馈
 * - 拖动时有倾斜效果增强3D感
 * - 支持自定义图标、颜色、大小和初始位置
 */
@Composable
fun BoxScope.DraggableFloatingButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomEnd,
    initialOffsetX: Float = 0f,  // 初始相对锚点的偏移(dp)
    initialOffsetY: Float = 0f,
    buttonSize: Dp = 52.dp,
    primaryColor: Color = Color(0xFF6366F1),
    highlightColor: Color = Color(0xFF818CF8),
    shadowColor: Color = Color(0xFF4F46E5)
) {
    val density = LocalDensity.current
    val fabSizePx = with(density) { buttonSize.toPx() }

    // 用于计算父容器尺寸
    var parentSize by remember { mutableStateOf(IntSize.Zero) }

    // 偏移量（相对于锚点位置）
    var offsetX by remember { mutableFloatStateOf(with(density) { initialOffsetX.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(with(density) { initialOffsetY.dp.toPx() }) }

    // 拖动状态
    var isDragging by remember { mutableStateOf(false) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    // 动画效果
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.15f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "scale"
    )

    // 3D倾斜效果（根据拖动方向）
    val rotationX by animateFloatAsState(
        targetValue = if (isDragging) (dragDelta.y * 0.1f).coerceIn(-15f, 15f) else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "rotationX"
    )
    val rotationY by animateFloatAsState(
        targetValue = if (isDragging) (-dragDelta.x * 0.1f).coerceIn(-15f, 15f) else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "rotationY"
    )

    // 高光位置根据倾斜变化
    val highlightOffsetX = rotationY * 0.5f
    val highlightOffsetY = -rotationX * 0.5f

    Box(
        modifier = modifier
            .align(alignment)
            .onSizeChanged { parentSize = it }
            .offset {
                IntOffset(
                    x = offsetX.roundToInt(),
                    y = offsetY.roundToInt()
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.rotationX = rotationX
                this.rotationY = rotationY
                cameraDistance = 12f * density.density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragDelta = Offset.Zero
                    },
                    onDragEnd = {
                        isDragging = false
                        dragDelta = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDelta = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDelta = dragAmount

                        // 更新位置
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        // 3D悬浮球
        Surface(
            onClick = onClick,
            shape = CircleShape,
            modifier = Modifier
                .size(buttonSize)
                .shadow(
                    elevation = if (isDragging) 16.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = primaryColor.copy(alpha = 0.3f),
                    spotColor = primaryColor.copy(alpha = 0.4f)
                ),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                highlightColor,  // 高光
                                primaryColor,    // 主色
                                shadowColor      // 阴影
                            ),
                            center = Offset(
                                x = fabSizePx * (0.35f + highlightOffsetX * 0.01f),
                                y = fabSizePx * (0.35f + highlightOffsetY * 0.01f)
                            ),
                            radius = fabSizePx * 0.8f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 高光层
                Box(
                    modifier = Modifier
                        .size(buttonSize * 0.85f)
                        .offset(
                            x = (-4 + highlightOffsetX * 0.3f).dp,
                            y = (-4 + highlightOffsetY * 0.3f).dp
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(fabSizePx * 0.3f, fabSizePx * 0.2f),
                                radius = fabSizePx * 0.5f
                            ),
                            shape = CircleShape
                        )
                )

                // 图标
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.size(buttonSize * 0.45f)
                )
            }
        }
    }
}

/**
 * 添加任务悬浮按钮（兼容旧接口）
 */
@Composable
fun BoxScope.DraggableFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialOffsetX: Float = -24f,
    initialOffsetY: Float = -100f
) {
    DraggableFloatingButton(
        onClick = onClick,
        icon = Icons.Default.Add,
        contentDescription = "添加任务",
        modifier = modifier,
        alignment = Alignment.BottomEnd,
        initialOffsetX = initialOffsetX,
        initialOffsetY = initialOffsetY,
        buttonSize = 56.dp,
        primaryColor = Color(0xFF6366F1),
        highlightColor = Color(0xFF818CF8),
        shadowColor = Color(0xFF4F46E5)
    )
}

/**
 * 搜索悬浮按钮
 */
@Composable
fun BoxScope.DraggableSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialOffsetX: Float = -16f,
    initialOffsetY: Float = 0f
) {
    DraggableFloatingButton(
        onClick = onClick,
        icon = Icons.Default.Search,
        contentDescription = "搜索任务",
        modifier = modifier,
        alignment = Alignment.TopEnd,
        initialOffsetX = initialOffsetX,
        initialOffsetY = initialOffsetY,
        buttonSize = 48.dp,
        primaryColor = Color(0xFF5C6BC0),
        highlightColor = Color(0xFF7986CB),
        shadowColor = Color(0xFF3F51B5)
    )
}

