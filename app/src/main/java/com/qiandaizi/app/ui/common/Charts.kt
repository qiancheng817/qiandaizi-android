package com.qiandaizi.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.money
import kotlin.math.cos
import kotlin.math.sin

data class Slice(val name: String, val value: Double, val color: Color)

/** 解析后端十六进制颜色（#RRGGBB / #AARRGGBB） */
fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching {
        val h = hex.removePrefix("#")
        when (h.length) {
            6 -> Color(0xFF000000 or h.toLong(16))
            8 -> Color(h.toLong(16))
            else -> null
        }
    }.getOrNull()
}

/** 环形占比图 + 中心金额 + 右侧图例（图例外置时 centerText 可显示总额） */
@Composable
fun DonutChart(
    slices: List<Slice>,
    centerText: String,
    centerSub: String = "",
    diameter: Dp = 160.dp,
    legend: Boolean = true
) {
    val total = slices.sumOf { it.value }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(diameter),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(diameter)) {
                val stroke = Stroke(width = size.minDimension * 0.22f)
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                var start = -90f
                if (total <= 0) {
                    drawArc(
                        color = Color(0xFFF0F1F3),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke
                    )
                    return@Canvas
                }
                slices.forEach { s ->
                    val sweep = (s.value / total * 360f).toFloat()
                    drawArc(
                        color = s.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Text(
                    centerText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                if (centerSub.isNotBlank()) {
                    androidx.compose.material3.Text(
                        centerSub, fontSize = 11.sp, color = TextSub
                    )
                }
            }
        }
        if (legend) {
            Spacer(Modifier.width(16.dp))
            Column(Modifier.fillMaxWidth()) {
                slices.take(6).forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .let { it }
                        ) {
                            Canvas(Modifier.size(9.dp)) {
                                drawCircle(s.color)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        androidx.compose.material3.Text(
                            s.name,
                            fontSize = 12.sp,
                            color = TextMain,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.Text(
                            if (total > 0) "${(s.value / total * 100).toInt()}%" else "-",
                            fontSize = 12.sp,
                            color = TextSub
                        )
                    }
                }
            }
        }
    }
}

/** 竖向柱状图（如月度/每日收支） */
@Composable
fun ColumnBars(
    items: List<Pair<String, Double>>,
    color: Color,
    height: Dp = 150.dp,
    moneyLabels: Boolean = false
) {
    val maxV = (items.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(0.01)
    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (items.isEmpty()) return@Canvas
            val gap = size.width / items.size
            val barW = gap * 0.55f
            items.forEachIndexed { i, (_, v) ->
                val h = (v / maxV * size.height).toFloat()
                val x = gap * i + (gap - barW) / 2f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2, barW / 2)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, pair ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text(
                        pair.first,
                        fontSize = 9.sp,
                        color = TextSub,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** 平滑折线/面积图（每日趋势） */
@Composable
fun TrendLine(
    points: List<Pair<String, Double>>,
    color: Color,
    height: Dp = 160.dp
) {
    val maxV = (points.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(0.01)
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (points.size < 2) return@Canvas
            val stepX = size.width / (points.size - 1)
            val coords = points.mapIndexed { i, (_, v) ->
                Offset(stepX * i, size.height - (v / maxV * size.height).toFloat())
            }
            val line = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val p0 = coords[i - 1]
                    val p1 = coords[i]
                    cubicTo(
                        p0.x + stepX / 2, p0.y,
                        p1.x - stepX / 2, p1.y,
                        p1.x, p1.y
                    )
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fill,
                color = color.copy(alpha = 0.12f)
            )
            drawPath(
                line,
                color = color,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            coords.forEach { c -> drawCircle(color, radius = 3.5.dp.toPx(), center = c) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            points.indices.step(((points.size / 6).coerceAtLeast(1))).forEach { i ->
                val pair = points[i]
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text(
                        pair.first, fontSize = 9.sp, color = TextSub, maxLines = 1
                    )
                }
            }
        }
    }
}
