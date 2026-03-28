package com.codex.sleepmonitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnsetDonutChart(
    insights: SessionInsights,
    modifier: Modifier = Modifier
) {
    val segments = listOf(
        "酝酿入睡" to (insights.settlingRatio to Color(0xFF55B6FF)),
        "平稳睡眠" to (insights.restfulRatio to Color(0xFF18C8A6)),
        "异常扰动" to (insights.disturbedRatio to Color(0xFFFF865B))
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(142.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                var startAngle = -90f
                segments.forEach { (_, pair) ->
                    val sweep = (pair.first.coerceAtLeast(0.04f) * 360f)
                    drawArc(
                        color = pair.second,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${insights.onsetLatencyMinutes}m",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "估算入睡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6A768B)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { (label, pair) ->
                LegendItem(
                    label = label,
                    color = pair.second,
                    value = "${(pair.first * 100).toInt()}%"
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendChart(
    points: List<WeeklyTrendPoint>,
    modifier: Modifier = Modifier
) {
    val maxDuration = (points.maxOfOrNull { it.durationHours } ?: 8f).coerceAtLeast(4f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val ratio = (point.durationHours / maxDuration).coerceIn(0.15f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = String.format("%.1fh", point.durationHours),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF536278)
                )
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height((110f * ratio).dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = Color(0xFF6A8BFF)
                ) {}
                Text(
                    text = point.label,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF526177)
                )
                Text(
                    text = "${point.onsetMinutes}m/${point.anomalyCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF7A889C)
                )
            }
        }
    }
}

@Composable
fun OnsetBarChart(
    points: List<OnsetBarPoint>,
    modifier: Modifier = Modifier
) {
    val maxValue = (points.maxOfOrNull { it.minutes } ?: 45).coerceAtLeast(20)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val ratio = point.minutes / maxValue.toFloat()
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "${point.minutes}m",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (point.highlight) Color(0xFF0F6A73) else Color(0xFF5E6C83)
                )
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height((120f * ratio.coerceIn(0.12f, 1f)).dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = if (point.highlight) Color(0xFF17BFB2) else Color(0xFFB8CBD9)
                ) {}
                Text(
                    text = point.label,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF526177)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
