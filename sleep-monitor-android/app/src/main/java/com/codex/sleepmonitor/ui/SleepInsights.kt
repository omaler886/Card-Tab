package com.codex.sleepmonitor.ui

import com.codex.sleepmonitor.data.CalibrationProfile
import com.codex.sleepmonitor.data.SleepEvent
import com.codex.sleepmonitor.data.SleepEventType
import com.codex.sleepmonitor.data.SleepLogEntry
import com.codex.sleepmonitor.data.SleepSession
import kotlin.math.roundToInt

data class SessionInsights(
    val onsetLatencyMinutes: Int,
    val settlingRatio: Float,
    val restfulRatio: Float,
    val disturbedRatio: Float,
    val eventCounts: Map<SleepEventType, Int>,
    val suggestions: List<String>
)

data class OnsetBarPoint(
    val label: String,
    val minutes: Int,
    val highlight: Boolean
)

data class WeeklyTrendPoint(
    val label: String,
    val durationHours: Float,
    val onsetMinutes: Int,
    val anomalyCount: Int
)

data class CorrelationInsight(
    val title: String,
    val detail: String
)

data class EventSeverity(
    val label: String,
    val colorHex: Long
)

fun sessionEvents(session: SleepSession): List<SleepEvent> {
    return if (session.sleepEvents.isNotEmpty()) {
        session.sleepEvents.sortedBy { it.timestampMillis }
    } else {
        session.snoreEvents.map {
            SleepEvent(
                type = SleepEventType.SNORE,
                timestampMillis = it.timestampMillis,
                durationMillis = it.durationMillis,
                intensity = it.intensity,
                peakDb = it.peakDb
            )
        }.sortedBy { it.timestampMillis }
    }
}

fun buildSessionInsights(session: SleepSession, now: Long): SessionInsights {
    val events = sessionEvents(session)
    val eventCounts = events.groupingBy { it.type }.eachCount()
    val onsetLatencyMinutes = estimateSleepOnsetLatencyMinutes(session, now)
    val sessionEnd = session.endedAtMillis ?: now
    val totalMinutes = ((sessionEnd - session.startedAtMillis).coerceAtLeast(60_000L) / 60_000L).toInt()
    val disturbedMinutes = session.soundBuckets.count {
        it.disturbanceScore > 0.45f ||
            it.snoreCount + it.talkCount + it.grindCount + it.ambientCount > 0
    } * 5
    val settlingMinutes = onsetLatencyMinutes.coerceAtMost(totalMinutes)
    val restfulMinutes = (totalMinutes - settlingMinutes - disturbedMinutes).coerceAtLeast(0)
    val totalRatioBase = (settlingMinutes + disturbedMinutes + restfulMinutes).coerceAtLeast(1)

    return SessionInsights(
        onsetLatencyMinutes = onsetLatencyMinutes,
        settlingRatio = settlingMinutes / totalRatioBase.toFloat(),
        restfulRatio = restfulMinutes / totalRatioBase.toFloat(),
        disturbedRatio = disturbedMinutes / totalRatioBase.toFloat(),
        eventCounts = eventCounts,
        suggestions = buildSuggestions(
            totalMinutes = totalMinutes,
            onsetLatencyMinutes = onsetLatencyMinutes,
            eventCounts = eventCounts,
            events = events
        )
    )
}

fun buildOnsetBarPoints(
    activeSession: SleepSession?,
    history: List<SleepSession>,
    now: Long
): List<OnsetBarPoint> {
    val sessions = buildList {
        activeSession?.let(::add)
        addAll(history.take(4))
    }.take(5)

    return sessions.mapIndexed { index, session ->
        OnsetBarPoint(
            label = if (index == 0 && activeSession != null) "本次" else "夜${index + 1}",
            minutes = estimateSleepOnsetLatencyMinutes(session, now),
            highlight = index == 0
        )
    }
}

fun buildWeeklyTrendPoints(
    sessions: List<SleepSession>,
    now: Long
): List<WeeklyTrendPoint> {
    return sessions.take(7).reversed().mapIndexed { index, session ->
        val end = session.endedAtMillis ?: now
        WeeklyTrendPoint(
            label = "D${index + 1}",
            durationHours = ((end - session.startedAtMillis).coerceAtLeast(1L) / 3_600_000f),
            onsetMinutes = estimateSleepOnsetLatencyMinutes(session, now),
            anomalyCount = sessionEvents(session).size
        )
    }
}

fun buildCorrelationInsights(
    sessions: List<SleepSession>,
    now: Long
): List<CorrelationInsight> {
    val sample = sessions.take(7)
    if (sample.size < 2) {
        return emptyList()
    }

    val insights = mutableListOf<CorrelationInsight>()

    compareSplit(sample, now, "咖啡因", { it.sleepLog.caffeineCups >= 2 }, "高咖啡因")?.let(insights::add)
    compareSplit(sample, now, "压力", { it.sleepLog.stressLevel >= 4 }, "高压力")?.let(insights::add)
    compareSplit(sample, now, "运动", { it.sleepLog.exerciseMinutes >= 20 }, "有运动")?.let(insights::add)
    compareBoolean(sample, "夜宵", { it.sleepLog.lateMeal })?.let(insights::add)
    compareBoolean(sample, "饮酒", { it.sleepLog.alcohol })?.let(insights::add)

    return insights.take(4)
}

fun buildCalibrationSummary(profile: CalibrationProfile): List<String> {
    if (profile.nightsAnalyzed == 0) {
        return listOf("还没有足够样本，先连续记录几晚，应用会自动根据你的卧室环境和异常分布调灵敏度。")
    }

    val summary = mutableListOf<String>()
    summary += "已完成 ${profile.nightsAnalyzed} 晚个人校准，基线噪声约 ${formatDb(profile.averageNoiseFloorDb)}。"
    if (profile.ambientThresholdOffset > 0.02f) {
        summary += "你的环境噪声偏高，环境异常检测阈值已自动调高，减少误报。"
    }
    if (profile.snoreThresholdOffset < 0f || profile.talkThresholdOffset < 0f) {
        summary += "卧室整体较安静，鼾声和梦话识别会更灵敏一些。"
    }
    if (profile.grindThresholdOffset > 0.02f) {
        summary += "磨牙触发阈值已略收紧，优先避免高频短促声的误判。"
    }
    return summary
}

fun buildShareReport(
    focusSession: SleepSession?,
    sessions: List<SleepSession>,
    calibrationProfile: CalibrationProfile,
    now: Long
): String {
    if (focusSession == null && sessions.isEmpty()) {
        return "NightPulse 暂无可分享的睡眠记录。"
    }

    val reference = focusSession ?: sessions.first()
    val insights = buildSessionInsights(reference, now)
    val weekly = buildWeeklyTrendPoints(sessions, now)
    val eventCounts = insights.eventCounts
    val end = reference.endedAtMillis ?: now
    val totalHours = ((end - reference.startedAtMillis).coerceAtLeast(1L) / 3_600_000f)
    val log = reference.sleepLog

    return buildString {
        appendLine("NightPulse 睡眠报告")
        appendLine("日期: ${formatDate(reference.startedAtMillis)}")
        appendLine("时长: ${String.format("%.1f", totalHours)} 小时")
        appendLine("估算入睡: ${insights.onsetLatencyMinutes} 分钟")
        appendLine("异常节点: ${sessionEvents(reference).size}")
        appendLine("鼾声: ${eventCounts[SleepEventType.SNORE] ?: 0}")
        appendLine("梦话: ${eventCounts[SleepEventType.DREAM_TALK] ?: 0}")
        appendLine("磨牙: ${eventCounts[SleepEventType.TEETH_GRINDING] ?: 0}")
        appendLine("环境异常: ${eventCounts[SleepEventType.AMBIENT_ALERT] ?: 0}")
        appendLine()
        appendLine("睡前日志")
        appendLine("咖啡因: ${log.caffeineCups} 杯")
        appendLine("压力: ${log.stressLevel}/5")
        appendLine("运动: ${log.exerciseMinutes} 分钟")
        appendLine("夜宵: ${if (log.lateMeal) "有" else "无"}")
        appendLine("饮酒: ${if (log.alcohol) "有" else "无"}")
        appendLine()
        appendLine("趋势摘要")
        if (weekly.isEmpty()) {
            appendLine("最近样本不足。")
        } else {
            appendLine("最近 ${weekly.size} 次平均时长 ${String.format("%.1f", weekly.map { it.durationHours }.average())} 小时")
            appendLine("最近 ${weekly.size} 次平均入睡 ${weekly.map { it.onsetMinutes }.average().roundToInt()} 分钟")
        }
        appendLine()
        appendLine("个人校准")
        appendLine("已校准 ${calibrationProfile.nightsAnalyzed} 晚，基线噪声 ${formatDb(calibrationProfile.averageNoiseFloorDb)}")
        appendLine()
        appendLine("建议")
        insights.suggestions.forEach { appendLine("- $it") }
    }.trim()
}

fun eventSeverity(intensity: Float, peakDb: Float): EventSeverity {
    return when {
        intensity >= 0.82f || peakDb >= -18f -> EventSeverity("重度", 0xFFE45C4B)
        intensity >= 0.55f || peakDb >= -28f -> EventSeverity("中度", 0xFFFFA14A)
        else -> EventSeverity("轻度", 0xFF17BFB2)
    }
}

fun estimateSleepOnsetLatencyMinutes(session: SleepSession, now: Long): Int {
    val sessionEnd = session.endedAtMillis ?: now
    if (sessionEnd <= session.startedAtMillis) {
        return 15
    }

    val earlyBuckets = session.soundBuckets
        .sortedBy { it.startMillis }
        .filter { it.startMillis - session.startedAtMillis <= 90 * 60_000L }

    if (earlyBuckets.size >= 2) {
        for (index in 0 until earlyBuckets.lastIndex) {
            val first = earlyBuckets[index]
            val second = earlyBuckets[index + 1]
            if (isQuietBucket(first) && isQuietBucket(second)) {
                val minutes = ((first.startMillis - session.startedAtMillis) / 60_000L).toInt()
                return minutes.coerceIn(8, 90)
            }
        }
    }

    val earlyEvents = sessionEvents(session).count {
        it.timestampMillis - session.startedAtMillis <= 60 * 60_000L
    }
    val averageDisturbance = earlyBuckets.map { it.disturbanceScore }.average().toFloat()
    return (
        12f + (earlyEvents * 5.5f) + (averageDisturbance * 24f)
        ).roundToInt().coerceIn(10, 75)
}

private fun isQuietBucket(bucket: com.codex.sleepmonitor.data.SoundBucket): Boolean {
    val anomalyCount = bucket.snoreCount + bucket.talkCount + bucket.grindCount + bucket.ambientCount
    return bucket.disturbanceScore < 0.28f &&
        anomalyCount == 0 &&
        bucket.averageDb < -42f
}

private fun buildSuggestions(
    totalMinutes: Int,
    onsetLatencyMinutes: Int,
    eventCounts: Map<SleepEventType, Int>,
    events: List<SleepEvent>
): List<String> {
    val suggestions = mutableListOf<String>()

    if (onsetLatencyMinutes >= 30) {
        suggestions += "入睡等待偏长，建议把咖啡因和高亮屏幕尽量提前到睡前 6 小时之外。"
    }
    if ((eventCounts[SleepEventType.AMBIENT_ALERT] ?: 0) >= 2) {
        suggestions += "环境异常偏多，建议检查门窗、空调噪声或夜间人声，尽量稳定卧室声场。"
    }
    if ((eventCounts[SleepEventType.TEETH_GRINDING] ?: 0) >= 1) {
        suggestions += "检测到磨牙迹象，若连续多晚出现，可以考虑牙套评估或留意近期压力负荷。"
    }
    if ((eventCounts[SleepEventType.DREAM_TALK] ?: 0) >= 2) {
        suggestions += "梦话片段较多，通常和疲劳或精神负荷有关，建议连续几晚提早休息时间。"
    }
    if ((eventCounts[SleepEventType.SNORE] ?: 0) >= 4 || events.any { it.type == SleepEventType.SNORE && it.intensity > 0.85f }) {
        suggestions += "鼾声偏频繁，侧睡、抬高头肩或观察鼻塞情况会更有帮助。"
    }
    if (totalMinutes in 1..359) {
        suggestions += "本次睡眠时长偏短，若是午睡，建议控制在 20 到 30 分钟更容易醒后清爽。"
    }

    return suggestions.take(4).ifEmpty {
        listOf("整体声音环境较平稳，继续保持固定就寝时间和较暗的卧室光线。")
    }
}

private fun compareSplit(
    sessions: List<SleepSession>,
    now: Long,
    title: String,
    predicate: (SleepSession) -> Boolean,
    highlightLabel: String
): CorrelationInsight? {
    val positive = sessions.filter(predicate)
    val negative = sessions.filterNot(predicate)
    if (positive.isEmpty() || negative.isEmpty()) {
        return null
    }

    val positiveOnset = positive.map { estimateSleepOnsetLatencyMinutes(it, now) }.average()
    val negativeOnset = negative.map { estimateSleepOnsetLatencyMinutes(it, now) }.average()
    val delta = (positiveOnset - negativeOnset).roundToInt()
    if (kotlin.math.abs(delta) < 4) {
        return null
    }

    return CorrelationInsight(
        title = "$title 关联",
        detail = "$highlightLabel 的晚上，平均入睡 ${if (delta > 0) "慢" else "快"} ${kotlin.math.abs(delta)} 分钟。"
    )
}

private fun compareBoolean(
    sessions: List<SleepSession>,
    title: String,
    selector: (SleepSession) -> Boolean
): CorrelationInsight? {
    val positive = sessions.filter(selector)
    val negative = sessions.filterNot(selector)
    if (positive.isEmpty() || negative.isEmpty()) {
        return null
    }

    val positiveAnomalies = positive.map { sessionEvents(it).size }.average()
    val negativeAnomalies = negative.map { sessionEvents(it).size }.average()
    val delta = (positiveAnomalies - negativeAnomalies).roundToInt()
    if (kotlin.math.abs(delta) < 1) {
        return null
    }

    return CorrelationInsight(
        title = "$title 关联",
        detail = "$title 出现的晚上，异常节点平均 ${if (delta > 0) "多" else "少"} ${kotlin.math.abs(delta)} 个。"
    )
}
