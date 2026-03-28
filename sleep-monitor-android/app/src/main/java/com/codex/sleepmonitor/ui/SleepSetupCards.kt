package com.codex.sleepmonitor.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codex.sleepmonitor.data.AutoStopPlan
import com.codex.sleepmonitor.data.BedtimePlan
import com.codex.sleepmonitor.data.NapPlan
import com.codex.sleepmonitor.data.SoothingSoundType
import com.codex.sleepmonitor.data.SleepLogEntry
import com.codex.sleepmonitor.data.SmartWakePlan
import com.codex.sleepmonitor.data.WhiteNoisePlan

@Composable
fun SleepLogCard(
    log: SleepLogEntry,
    onCaffeineChanged: (Int) -> Unit,
    onStressChanged: (Int) -> Unit,
    onExerciseChanged: (Int) -> Unit,
    onLateMealToggled: () -> Unit,
    onAlcoholToggled: () -> Unit
) {
    GlassCard(title = "睡前日志", icon = Icons.Rounded.Bedtime) {
        Text(
            text = "这些记录会自动快照到本次睡眠里，用来做后面的趋势和关联分析。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C6A82)
        )
        Spacer(modifier = Modifier.size(12.dp))
        StepperRow("咖啡因", "${log.caffeineCups} 杯", { onCaffeineChanged((log.caffeineCups - 1).coerceAtLeast(0)) }, { onCaffeineChanged((log.caffeineCups + 1).coerceAtMost(6)) })
        StepperRow("压力", "${log.stressLevel}/5", { onStressChanged((log.stressLevel - 1).coerceAtLeast(1)) }, { onStressChanged((log.stressLevel + 1).coerceAtMost(5)) })
        StepperRow("运动", "${log.exerciseMinutes} 分钟", { onExerciseChanged((log.exerciseMinutes - 10).coerceAtLeast(0)) }, { onExerciseChanged((log.exerciseMinutes + 10).coerceAtMost(180)) })
        Spacer(modifier = Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleChip("夜宵", log.lateMeal, onLateMealToggled)
            ToggleChip("饮酒", log.alcohol, onAlcoholToggled)
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TinyActionButton("-") { onMinus() }
            Text(value, style = MaterialTheme.typography.titleMedium)
            TinyActionButton("+") { onPlus() }
        }
    }
}

@Composable
private fun TinyActionButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFE1EEF5),
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun ToggleChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active) Color(0xFF143B5B) else Color(0xFFE1EEF5),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (active) Color.White else Color(0xFF38506A),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SmartWakeCard(
    smartWakePlan: SmartWakePlan?,
    alarmTriggered: Boolean,
    now: Long,
    onStartSmartWakeRequested: (Int, Int) -> Unit,
    onCancelSmartWakeRequested: () -> Unit,
    onStopAlarmRequested: () -> Unit
) {
    GlassCard(title = "智能唤醒", icon = Icons.Rounded.Alarm) {
        if (smartWakePlan == null) {
            Text(
                text = "不是死板到点响铃，而是在你设定的窗口里挑更可能醒得轻松的时刻叫醒。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("6h / 30m") { onStartSmartWakeRequested(6, 30) }
                SmartWakeButton("7h / 30m") { onStartSmartWakeRequested(7, 30) }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("8h / 30m") { onStartSmartWakeRequested(8, 30) }
                SmartWakeButton("8h / 45m") { onStartSmartWakeRequested(8, 45) }
            }
        } else {
            MetricLine("目标时刻", formatClock(smartWakePlan.targetWakeAtMillis))
            MetricLine("唤醒窗口", "${smartWakePlan.windowMinutes} 分钟")
            MetricLine(
                if (alarmTriggered) "当前状态" else "剩余时间",
                if (alarmTriggered) "已触发铃声" else formatDuration(smartWakePlan.targetWakeAtMillis - now)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Button(
                onClick = if (alarmTriggered) onStopAlarmRequested else onCancelSmartWakeRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (alarmTriggered) Color(0xFFD95B47) else Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text(if (alarmTriggered) "停止铃声" else "取消智能唤醒")
            }
        }
    }
}

@Composable
private fun SmartWakeButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(132.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF143B5B),
            contentColor = Color.White
        )
    ) {
        Text(label)
    }
}

@Composable
fun NapAlarmCard(
    napPlan: NapPlan?,
    now: Long,
    onStartNapRequested: (Int) -> Unit,
    onCancelNapRequested: () -> Unit,
    onStopAlarmRequested: () -> Unit
) {
    GlassCard(title = "午睡唤醒器", icon = Icons.Rounded.Alarm) {
        val triggered = napPlan?.triggeredAtMillis != null
        if (napPlan == null) {
            Text(
                text = "给午睡、碎片休息和短时间充电准备的快速唤醒器，建议优先使用 20 到 30 分钟。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickNapButton("15 分钟") { onStartNapRequested(15) }
                QuickNapButton("20 分钟") { onStartNapRequested(20) }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickNapButton("30 分钟") { onStartNapRequested(30) }
                QuickNapButton("45 分钟") { onStartNapRequested(45) }
            }
        } else {
            MetricLine(
                if (triggered) "当前状态" else "剩余时间",
                if (triggered) "铃声提醒中" else formatDuration(napPlan.wakeAtMillis - now)
            )
            Spacer(modifier = Modifier.size(14.dp))
            Button(
                onClick = if (triggered) onStopAlarmRequested else onCancelNapRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (triggered) Color(0xFFD95B47) else Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text(if (triggered) "停止铃声" else "取消午睡")
            }
        }
    }
}

@Composable
private fun QuickNapButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(132.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF143B5B),
            contentColor = Color.White
        )
    ) {
        Text(label)
    }
}

@Composable
fun GuidanceCard(
    alarmTriggered: Boolean,
    onStopAlarmRequested: () -> Unit,
    onOpenSettings: (Intent) -> Unit
) {
    GlassCard(title = "连续运行建议", icon = Icons.Rounded.Bedtime) {
        Text(
            text = "已经接入前台常驻服务、整夜录音分析、智能唤醒和午睡唤醒。首次安装后，建议把电池优化加入白名单，夜间会更稳。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5C6A82)
        )
        Spacer(modifier = Modifier.size(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    onOpenSettings(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text("电池优化设置")
            }
            if (alarmTriggered) {
                Button(
                    onClick = onStopAlarmRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD95B47),
                        contentColor = Color.White
                    )
                ) {
                    Text("停止当前铃声")
                }
            }
        }
    }
}

@Composable
fun WhiteNoiseCard(
    whiteNoisePlan: WhiteNoisePlan?,
    now: Long,
    onStartWhiteNoiseRequested: (SoothingSoundType, Int, Int?) -> Unit,
    onStopWhiteNoiseRequested: () -> Unit
) {
    GlassCard(title = "白噪音助眠", icon = Icons.Rounded.GraphicEq) {
        if (whiteNoisePlan == null) {
            Text(
                text = "现在可以切换多种助眠音: 白噪音、雨声、海浪、风扇声。定时结束时会渐弱停止。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("雨声 30m") { onStartWhiteNoiseRequested(SoothingSoundType.RAIN, 28, 30) }
                SmartWakeButton("海浪 30m") { onStartWhiteNoiseRequested(SoothingSoundType.OCEAN, 28, 30) }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("风扇 持续") { onStartWhiteNoiseRequested(SoothingSoundType.FAN, 22, null) }
                SmartWakeButton("白噪音 持续") { onStartWhiteNoiseRequested(SoothingSoundType.WHITE_NOISE, 30, null) }
            }
        } else {
            MetricLine("当前音色", soothingSoundLabel(whiteNoisePlan.soundType))
            MetricLine("当前音量", "${whiteNoisePlan.volumePercent}%")
            MetricLine(
                "停止时间",
                whiteNoisePlan.stopAtMillis?.let { formatClock(it) } ?: "手动停止"
            )
            whiteNoisePlan.stopAtMillis?.let {
                MetricLine("剩余时间", formatDuration(it - now))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Button(
                onClick = onStopWhiteNoiseRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text("停止白噪音")
            }
        }
    }
}

private fun soothingSoundLabel(type: SoothingSoundType): String = when (type) {
    SoothingSoundType.WHITE_NOISE -> "白噪音"
    SoothingSoundType.RAIN -> "雨声"
    SoothingSoundType.OCEAN -> "海浪"
    SoothingSoundType.FAN -> "风扇声"
}

@Composable
fun AutoStopCard(
    autoStopPlan: AutoStopPlan?,
    now: Long,
    onStartAutoStopRequested: (Int) -> Unit,
    onCancelAutoStopRequested: () -> Unit
) {
    GlassCard(title = "自动结束监测", icon = Icons.Rounded.Timer) {
        if (autoStopPlan == null) {
            Text(
                text = "到达预设时长后自动结束监控，适合你想固定睡满一段时间时使用。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("2 小时") { onStartAutoStopRequested(2) }
                SmartWakeButton("4 小时") { onStartAutoStopRequested(4) }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("6 小时") { onStartAutoStopRequested(6) }
                SmartWakeButton("8 小时") { onStartAutoStopRequested(8) }
            }
        } else {
            MetricLine("预设时长", "${autoStopPlan.durationHours} 小时")
            MetricLine("结束时刻", formatClock(autoStopPlan.stopAtMillis))
            MetricLine("剩余时间", formatDuration(autoStopPlan.stopAtMillis - now))
            Spacer(modifier = Modifier.size(12.dp))
            Button(
                onClick = onCancelAutoStopRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text("取消自动结束")
            }
        }
    }
}

@Composable
fun BedtimePlanCard(
    bedtimePlan: BedtimePlan?,
    onSetBedtimePlanRequested: (Int, Int, Boolean) -> Unit,
    onClearBedtimePlanRequested: () -> Unit
) {
    var autoStart by remember(bedtimePlan?.autoStart) { mutableStateOf(bedtimePlan?.autoStart ?: false) }

    GlassCard(title = "睡前计划", icon = Icons.Rounded.Schedule) {
        if (bedtimePlan == null) {
            Text(
                text = "可以每天固定时间提醒你准备入睡，或者直接在那个时间自动开始监测。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5C6A82)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleChip("仅提醒", !autoStart) { autoStart = false }
                ToggleChip("自动开始", autoStart) { autoStart = true }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("22:30") { onSetBedtimePlanRequested(22, 30, autoStart) }
                SmartWakeButton("23:00") { onSetBedtimePlanRequested(23, 0, autoStart) }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmartWakeButton("23:30") { onSetBedtimePlanRequested(23, 30, autoStart) }
                SmartWakeButton("00:00") { onSetBedtimePlanRequested(0, 0, autoStart) }
            }
        } else {
            MetricLine("每日时间", "%02d:%02d".format(bedtimePlan.hour, bedtimePlan.minute))
            MetricLine("计划模式", if (bedtimePlan.autoStart) "自动开始监测" else "仅提醒")
            MetricLine("下一次", formatClock(bedtimePlan.nextTriggerAtMillis))
            Spacer(modifier = Modifier.size(12.dp))
            Button(
                onClick = onClearBedtimePlanRequested,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF143B5B),
                    contentColor = Color.White
                )
            ) {
                Text("关闭睡前计划")
            }
        }
    }
}
