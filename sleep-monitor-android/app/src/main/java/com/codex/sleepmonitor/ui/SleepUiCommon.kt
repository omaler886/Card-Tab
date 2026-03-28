package com.codex.sleepmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.sleepmonitor.data.SleepEventType
import com.codex.sleepmonitor.data.SleepSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HeaderBlock() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0x3317D6D1)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = Color(0xFFB5FFF7)
            )
        }
        Column {
            Text(
                text = "NightPulse",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "智能唤醒 · 片段回放 · 趋势关联 · 个人校准",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC9D8EC)
            )
        }
    }
}

@Composable
fun HeroCard(
    activeSession: SleepSession?,
    insights: SessionInsights?,
    now: Long,
    onStartRequested: () -> Unit,
    onStopRequested: () -> Unit
) {
    val monitoring = activeSession != null
    val elapsedLabel = activeSession?.let { formatDuration(now - it.startedAtMillis) } ?: "00:00:00"
    val buttonText = if (monitoring) "结束整夜监测" else "开始整夜监测"
    val buttonIcon = if (monitoring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow
    val anomalyTotal = activeSession?.sleepEvents?.size ?: 0

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF10203D), Color(0xFF175C74), Color(0xFF13B6A9))
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (monitoring) "整夜音频正在持续分析" else "今晚准备开始监听",
                            color = Color(0xFFDDF9F8),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = elapsedLabel,
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    ScoreBubble(score = insights?.onsetLatencyMinutes ?: 18)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsightPill(title = "估算入睡", value = "${insights?.onsetLatencyMinutes ?: 18} 分钟")
                    InsightPill(title = "异常节点", value = "$anomalyTotal")
                    InsightPill(title = "录音片段", value = "${activeSession?.anomalyClips?.size ?: 0} 段")
                }

                Button(
                    onClick = if (monitoring) onStopRequested else onStartRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0E3655)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Icon(buttonIcon, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(buttonText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ScoreBubble(score: Int) {
    Surface(
        shape = CircleShape,
        color = Color(0x33FFFFFF)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "入睡分钟",
                color = Color(0xFFD5F5F4),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun InsightPill(title: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0x26FFFFFF)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0xFFD5E7EE),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun GlassCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE2F4F5)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = Color(0xFF126C76)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

@Composable
fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C6A82)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun eventTypeLabel(type: SleepEventType): String = when (type) {
    SleepEventType.SNORE -> "鼾声"
    SleepEventType.DREAM_TALK -> "梦话"
    SleepEventType.TEETH_GRINDING -> "磨牙"
    SleepEventType.AMBIENT_ALERT -> "环境异常"
}

fun eventColor(type: SleepEventType): Color = when (type) {
    SleepEventType.SNORE -> Color(0xFF17BFB2)
    SleepEventType.DREAM_TALK -> Color(0xFF5E93FF)
    SleepEventType.TEETH_GRINDING -> Color(0xFFFFA14A)
    SleepEventType.AMBIENT_ALERT -> Color(0xFFE45C4B)
}

fun eventIcon(type: SleepEventType): ImageVector = when (type) {
    SleepEventType.SNORE -> Icons.Rounded.GraphicEq
    SleepEventType.DREAM_TALK -> Icons.Rounded.RecordVoiceOver
    SleepEventType.TEETH_GRINDING -> Icons.Rounded.Bolt
    SleepEventType.AMBIENT_ALERT -> Icons.Rounded.WarningAmber
}

fun formatDuration(durationMillis: Long): String {
    val safe = durationMillis.coerceAtLeast(0L)
    val totalSeconds = safe / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatClock(timestampMillis: Long): String = Instant.ofEpochMilli(timestampMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatDate(timestampMillis: Long): String = Instant.ofEpochMilli(timestampMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("MM-dd"))

fun formatDb(value: Float): String = String.format(Locale.US, "%.1f dBFS", value)
