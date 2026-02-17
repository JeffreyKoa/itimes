package com.xltool.quadrant.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.PI

/**
 * 标签统计数据
 */
data class TagData(
    val name: String,
    val count: Int
)

/**
 * 雷达图组件
 * @param data 标签数据列表（最多8个）
 * @param maxValue 最大值（用于归一化）
 * @param gridColor 网格颜色
 * @param fillColor 填充颜色
 * @param strokeColor 边框颜色
 */
@Composable
fun RadarChart(
    data: List<TagData>,
    maxValue: Int,
    modifier: Modifier = Modifier,
    gridColor: Color = Color.Gray.copy(alpha = 0.3f),
    fillColor: Color = Color(0xFF667eea).copy(alpha = 0.3f),
    strokeColor: Color = Color(0xFF667eea)
) {
    // 至少需要3个数据点才能绘制
    if (data.size < 3 || maxValue <= 0) return
    
    val limitedData = data.take(8) // 最多8个顶点
    val numPoints = limitedData.size
    
    // 动画进度
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "radar_animation"
    )
    
    LaunchedEffect(data) {
        animationProgress = 0f
        animationProgress = 1f
    }
    
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(size.width, size.height) / 2 * 0.75f
        
        // 计算每个顶点的角度（从顶部开始，顺时针）
        val angleStep = (2 * PI / numPoints).toFloat()
        val startAngle = (-PI / 2).toFloat() // 从顶部开始
        
        // 绘制同心圆网格（4层）
        for (level in 1..4) {
            val levelRadius = radius * level / 4
            val gridPath = Path()
            for (i in 0 until numPoints) {
                val angle = startAngle + i * angleStep
                val x = centerX + levelRadius * cos(angle)
                val y = centerY + levelRadius * sin(angle)
                if (i == 0) {
                    gridPath.moveTo(x, y)
                } else {
                    gridPath.lineTo(x, y)
                }
            }
            gridPath.close()
            drawPath(
                path = gridPath,
                color = gridColor,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // 绘制从中心到各顶点的射线
        for (i in 0 until numPoints) {
            val angle = startAngle + i * angleStep
            val endX = centerX + radius * cos(angle)
            val endY = centerY + radius * sin(angle)
            drawLine(
                color = gridColor,
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // 绘制数据区域
        val dataPath = Path()
        limitedData.forEachIndexed { i, tagData ->
            val angle = startAngle + i * angleStep
            val normalizedValue = (tagData.count.toFloat() / maxValue).coerceIn(0f, 1f)
            val valueRadius = radius * normalizedValue * animatedProgress
            val x = centerX + valueRadius * cos(angle)
            val y = centerY + valueRadius * sin(angle)
            if (i == 0) {
                dataPath.moveTo(x, y)
            } else {
                dataPath.lineTo(x, y)
            }
        }
        dataPath.close()
        
        // 填充
        drawPath(
            path = dataPath,
            color = fillColor
        )
        
        // 边框
        drawPath(
            path = dataPath,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // 绘制数据点
        limitedData.forEachIndexed { i, tagData ->
            val angle = startAngle + i * angleStep
            val normalizedValue = (tagData.count.toFloat() / maxValue).coerceIn(0f, 1f)
            val valueRadius = radius * normalizedValue * animatedProgress
            val x = centerX + valueRadius * cos(angle)
            val y = centerY + valueRadius * sin(angle)
            
            // 绘制数据点圆圈
            drawCircle(
                color = strokeColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
        
        // 绘制标签文字
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor(
                String.format("#%06X", 0xFFFFFF and textColor.hashCode())
            )
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        
        limitedData.forEachIndexed { i, tagData ->
            val angle = startAngle + i * angleStep
            val labelRadius = radius * 1.15f
            val x = centerX + labelRadius * cos(angle)
            val y = centerY + labelRadius * sin(angle)
            
            drawContext.canvas.nativeCanvas.drawText(
                "${tagData.name}(${tagData.count})",
                x,
                y + textPaint.textSize / 3,
                textPaint
            )
        }
    }
}
