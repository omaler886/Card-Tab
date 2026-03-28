package com.codex.sleepmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codex.sleepmonitor.data.AnomalyClip
import com.codex.sleepmonitor.data.SleepEvent
import com.codex.sleepmonitor.data.SleepEventType
import com.codex.sleepmonitor.data.SleepSession
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AnomalyTimelineCard(
    session: SleepSession?,
    events: List<SleepEvent>
) {
    GlassCard(title = "异常时间节点", icon = Icons.Rounded.WarningAmber) {
        if (session == null || events.isEmpty()) {
            Text(
                text = "开始监测后，鼾声、梦话、磨牙和环境异常都会以节点形式落在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            return@GlassCard
        }

        var selectedType by remember(session.id, events.size) { mutableStateOf<SleepEventType?>(null) }
        val filteredEvents = events.filter { selectedType == null || it.type == selectedType }
        EventFilterRow(selectedType) { selectedType = it }

        val end = session.endedAtMillis ?: System.currentTimeMillis()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2EBF3))
            )
            filteredEvents.takeLast(8).forEach { event ->
                val ratio = ((event.timestampMillis - session.startedAtMillis).toFloat() /
                    (end - session.startedAtMillis).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                val xOffset = (maxWidth - 28.dp) * ratio
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = xOffset),
                    shape = CircleShape,
                    color = eventColor(event.type)
                ) {
                    Icon(
                        imageVector = eventIcon(event.type),
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp).size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        filteredEvents.takeLast(5).reversed().forEach { event ->
            val severity = eventSeverity(event.intensity, event.peakDb)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = eventColor(event.type)) {
                        Icon(
                            imageVector = eventIcon(event.type),
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = eventTypeLabel(event.type),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        SeverityPill(severity)
                        Text(
                            text = "持续 ${String.format(Locale.US, "%.1f", event.durationMillis / 1000f)} 秒 · 强度 ${(event.intensity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6F7C91)
                        )
                    }
                }
                Text(
                    text = formatClock(event.timestampMillis),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun ClipPlaybackCard(
    clips: List<AnomalyClip>,
    playingClipId: String?,
    onPlayClip: (AnomalyClip) -> Unit
) {
    GlassCard(title = "异常片段回放", icon = Icons.Rounded.RecordVoiceOver) {
        if (clips.isEmpty()) {
            Text(
                text = "命中异常后，会把最近几秒录成可回放 WAV 片段，方便你复盘具体发生了什么。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
        } else {
            var selectedType by remember(clips.size) { mutableStateOf<SleepEventType?>(null) }
            EventFilterRow(selectedType) { selectedType = it }
            clips.filter { selectedType == null || it.eventType == selectedType }.forEach { clip ->
                val severity = eventSeverity(((clip.peakDb + 46f) / 30f).coerceIn(0f, 1f), clip.peakDb)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${eventTypeLabel(clip.eventType)} · ${formatClock(clip.capturedAtMillis)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        SeverityPill(severity)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", clip.durationMillis / 1000f)} 秒 · 峰值 ${formatDb(clip.peakDb)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6F7C91)
                        )
                    }
                    Button(
                        onClick = { onPlayClip(clip) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (playingClipId == clip.id) Color(0xFFD95B47) else Color(0xFF143B5B),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (playingClipId == clip.id) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (playingClipId == clip.id) "停止" else "播放")
                    }
                }
            }
        }
    }
}

@Composable
private fun EventFilterRow(
    selectedType: SleepEventType?,
    onSelect: (SleepEventType?) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleChip("全部", selectedType == null) { onSelect(null) }
        ToggleChip("鼾声", selectedType == SleepEventType.SNORE) { onSelect(SleepEventType.SNORE) }
        ToggleChip("梦话", selectedType == SleepEventType.DREAM_TALK) { onSelect(SleepEventType.DREAM_TALK) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleChip("磨牙", selectedType == SleepEventType.TEETH_GRINDING) { onSelect(SleepEventType.TEETH_GRINDING) }
        ToggleChip("环境", selectedType == SleepEventType.AMBIENT_ALERT) { onSelect(SleepEventType.AMBIENT_ALERT) }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun SeverityPill(severity: EventSeverity) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(severity.colorHex)
    ) {
        Text(
            text = severity.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun CalibrationCard(
    summary: List<String>,
    nightsAnalyzed: Int,
    onRecalibrateRequested: () -> Unit
) {
    GlassCard(title = "个人校准", icon = Icons.Rounded.Tune) {
        MetricLine("已校准夜数", "$nightsAnalyzed 晚")
        Spacer(modifier = Modifier.height(10.dp))
        summary.forEach { item ->
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
                        .background(Color(0xFF17BFB2), CircleShape)
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5C6A82)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRecalibrateRequested,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF143B5B),
                contentColor = Color.White
            )
        ) {
            Text("重新校准")
        }
    }
}

@Composable
fun BackupCard(
    onExportEncryptedBackupRequested: () -> Unit,
    onImportEncryptedBackupRequested: () -> Unit
) {
    GlassCard(title = "云端备份 / 本地加密", icon = Icons.Rounded.CloudUpload) {
        Text(
            text = "应用状态文件已经按本地密钥加密落盘。这里可以导出加密备份包，或从备份包恢复历史记录和异常片段。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C6A82)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onExportEncryptedBackupRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text("导出加密备份")
            }
            Button(
                onClick = onImportEncryptedBackupRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C5F88),
                    contentColor = Color.White
                )
            ) {
                Text("恢复导入")
            }
        }
    }
}

@Composable
fun SleepSuggestionsCard(insights: SessionInsights?) {
    GlassCard(title = "睡眠建议", icon = Icons.Rounded.Lightbulb) {
        val suggestions = insights?.suggestions.orEmpty()
        if (suggestions.isEmpty()) {
            Text(
                text = "等本次睡眠记录结束后，这里会给出面向入睡、噪声和异常事件的建议。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
        } else {
            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(8.dp)
                            .background(Color(0xFF18C8A6), CircleShape)
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4E5D73)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(
    sessions: List<SleepSession>,
    now: Long
) {
    GlassCard(title = "最近睡眠记录", icon = Icons.Rounded.History) {
        if (sessions.isEmpty()) {
            Text(
                text = "还没有完成的夜间记录。开始一次监测后，异常类型、日志快照和入睡分析都会沉淀在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
        } else {
            sessions.take(4).forEach { session ->
                val insights = buildSessionInsights(session, now)
                val anomalyCount = sessionEvents(session).size
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formatDate(session.startedAtMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatClock(session.startedAtMillis)} - ${
                                session.endedAtMillis?.let(::formatClock) ?: "--:--"
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6F7C91)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatDuration(
                                (session.endedAtMillis ?: session.startedAtMillis) - session.startedAtMillis
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "入睡 ${insights.onsetLatencyMinutes} 分钟 · 节点 $anomalyCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6F7C91)
                        )
                    }
                }
            }
        }
    }
}
