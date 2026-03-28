package com.codex.sleepmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.sleepmonitor.data.SleepEvent
import com.codex.sleepmonitor.data.SleepEventType
import com.codex.sleepmonitor.data.SleepSession
import kotlin.math.roundToInt

@Composable
fun LiveSignalsCard(
    activeSession: SleepSession?,
    focusSession: SleepSession?,
    events: List<SleepEvent>
) {
    val session = activeSession ?: focusSession
    val confidence = activeSession?.latestConfidence ?: 0f
    val disturbance = activeSession?.latestDisturbance ?: session?.soundBuckets?.lastOrNull()?.disturbanceScore ?: 0f
    val latestEvent = events.lastOrNull()
    val eventCounts = events.groupingBy { it.type }.eachCount()

    GlassCard(title = "多事件监听强度", icon = Icons.Rounded.GraphicEq) {
        Text(
            text = "实时音频会持续分析鼾声、梦话、磨牙和周围突发环境声，并把异常打到时间节点上。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C6A82)
        )
        Spacer(modifier = Modifier.height(16.dp))
        MetricLine("当前异常置信度", "${(confidence * 100).roundToInt()}%")
        LinearProgressIndicator(
            progress = { confidence.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF11BFB4),
            trackColor = Color(0xFFE4EEF5)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MetricLine("扰动得分", "${(disturbance * 100).roundToInt()}%")
        Spacer(modifier = Modifier.height(12.dp))
        MetricLine(
            "最近事件",
            latestEvent?.let { "${eventTypeLabel(it.type)} ${formatClock(it.timestampMillis)}" } ?: "暂无"
        )
        Spacer(modifier = Modifier.height(16.dp))
        EventSummaryRow("鼾声", eventCounts[SleepEventType.SNORE] ?: 0, Color(0xFF17BFB2))
        EventSummaryRow("梦话", eventCounts[SleepEventType.DREAM_TALK] ?: 0, Color(0xFF6398FF))
        EventSummaryRow("磨牙", eventCounts[SleepEventType.TEETH_GRINDING] ?: 0, Color(0xFFFFA14A))
        EventSummaryRow("环境", eventCounts[SleepEventType.AMBIENT_ALERT] ?: 0, Color(0xFFE45C4B))
    }
}

@Composable
private fun EventSummaryRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text("$count 次", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SleepAnalysisCard(
    insights: SessionInsights?,
    onsetPoints: List<OnsetBarPoint>
) {
    GlassCard(title = "入睡时间分析", icon = Icons.Rounded.AutoGraph) {
        if (insights == null) {
            Text(
                text = "开始一段睡眠记录后，这里会用环状图和柱状图分析估算入睡时间与扰动比例。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
        } else {
            OnsetDonutChart(insights = insights)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "最近入睡对比",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OnsetBarChart(points = onsetPoints)
        }
    }
}

@Composable
fun WeeklyTrendCard(
    points: List<WeeklyTrendPoint>,
    correlations: List<CorrelationInsight>,
    onShareReportRequested: () -> Unit,
    onExportWeeklyPdfRequested: () -> Unit,
    onExportWeeklyImageRequested: () -> Unit
) {
    GlassCard(title = "每周趋势页", icon = Icons.Rounded.AutoGraph) {
        if (points.isEmpty()) {
            Text(
                text = "连续记录几晚后，这里会生成最近 7 次的睡眠时长、入睡速度和异常趋势。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
        } else {
            Text(
                text = "柱状图显示最近 7 次睡眠时长，底部附带“入睡分钟 / 异常数”。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            Spacer(modifier = Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = onShareReportRequested,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text("分享睡眠报告")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.Button(
                    onClick = onExportWeeklyPdfRequested,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C5F88),
                        contentColor = Color.White
                    )
                ) {
                    Text("导出 PDF")
                }
                androidx.compose.material3.Button(
                    onClick = onExportWeeklyImageRequested,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A7DA1),
                        contentColor = Color.White
                    )
                ) {
                    Text("导出图片")
                }
            }
            WeeklyTrendChart(points = points)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "日志关联",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (correlations.isEmpty()) {
                Text(
                    text = "当前样本还不够明显，继续记录睡前日志后，应用会自动总结关联规律。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6F7C91)
                )
            } else {
                correlations.forEach { insight ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(8.dp)
                                .background(Color(0xFF6A8BFF), CircleShape)
                        )
                        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = insight.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5C6A82)
                            )
                        }
                    }
                }
            }
        }
    }
}
