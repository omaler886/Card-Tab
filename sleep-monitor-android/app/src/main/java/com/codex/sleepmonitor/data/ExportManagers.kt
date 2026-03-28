package com.codex.sleepmonitor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.codex.sleepmonitor.ui.buildSessionInsights
import com.codex.sleepmonitor.ui.buildShareReport
import com.codex.sleepmonitor.ui.buildWeeklyTrendPoints
import com.codex.sleepmonitor.ui.formatDate
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val exportJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

object ReportExportManager {
    fun exportWeeklyPdf(
        context: Context,
        store: SleepStore,
        now: Long = System.currentTimeMillis()
    ): File {
        val file = File(context.cacheDir, "exports/nightpulse-weekly-report.pdf")
        file.parentFile?.mkdirs()

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 250, 253) }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 57, 90) }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(23, 191, 178) }
        val accentSoftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(104, 144, 255) }
        val textDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 53, 74)
            textSize = 32f
        }
        val textBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 88, 107)
            textSize = 24f
        }
        val textTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f
            isFakeBoldText = true
        }
        val textCardTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 56, 91)
            textSize = 34f
            isFakeBoldText = true
        }

        canvas.drawRect(0f, 0f, 1240f, 1754f, bgPaint)
        canvas.drawRoundRect(RectF(0f, 0f, 1240f, 250f), 0f, 0f, headerPaint)
        canvas.drawText("NightPulse Weekly Report", 70f, 105f, textTitle)
        canvas.drawText("周报日期 ${formatDate(now)}", 70f, 158f, textBody.apply { color = Color.rgb(214, 235, 244) })

        val reference = store.activeSession ?: store.sessions.firstOrNull()
        val insights = reference?.let { buildSessionInsights(it, now) }
        val weekly = buildWeeklyTrendPoints(store.sessions, now)

        drawCard(canvas, cardPaint, 60f, 290f, 1120f, 280f)
        canvas.drawText("总览", 90f, 345f, textCardTitle)
        val summaryLines = listOf(
            "最近一晚时长 ${reference?.let { formatDuration((it.endedAtMillis ?: now) - it.startedAtMillis) } ?: "--:--"}",
            "估算入睡 ${insights?.onsetLatencyMinutes ?: 0} 分钟",
            "异常节点 ${reference?.sleepEvents?.size ?: 0}",
            "校准夜数 ${store.calibrationProfile.nightsAnalyzed}"
        )
        summaryLines.forEachIndexed { index, line ->
            canvas.drawText(line, 90f + (index % 2) * 500f, 410f + (index / 2) * 70f, textDark)
        }

        drawCard(canvas, cardPaint, 60f, 600f, 1120f, 470f)
        canvas.drawText("最近 7 次趋势", 90f, 655f, textCardTitle)
        val chartBaseY = 980f
        weekly.forEachIndexed { index, point ->
            val left = 105f + index * 145f
            val barHeight = (point.durationHours * 58f).coerceAtMost(300f)
            canvas.drawRoundRect(RectF(left, chartBaseY - barHeight, left + 78f, chartBaseY), 18f, 18f, if (index % 2 == 0) accentPaint else accentSoftPaint)
            canvas.drawText(point.label, left, chartBaseY + 45f, textBody)
            canvas.drawText("${point.onsetMinutes}m", left, chartBaseY + 80f, textBody)
            canvas.drawText("${point.anomalyCount} 节点", left, chartBaseY + 115f, textBody)
        }

        drawCard(canvas, cardPaint, 60f, 1110f, 1120f, 560f)
        canvas.drawText("日志与建议", 90f, 1165f, textCardTitle)
        val log = reference?.sleepLog
        val logLines = listOf(
            "咖啡因 ${log?.caffeineCups ?: 0} 杯",
            "压力 ${log?.stressLevel ?: 0}/5",
            "运动 ${log?.exerciseMinutes ?: 0} 分钟",
            "夜宵 ${if (log?.lateMeal == true) "有" else "无"} / 饮酒 ${if (log?.alcohol == true) "有" else "无"}"
        )
        logLines.forEachIndexed { index, line ->
            canvas.drawText(line, 90f, 1235f + index * 48f, textDark)
        }
        insights?.suggestions?.take(4)?.forEachIndexed { index, suggestion ->
            canvas.drawCircle(98f, 1440f + index * 56f, 7f, accentPaint)
            canvas.drawText(suggestion.take(42), 120f, 1450f + index * 56f, textBody)
        }

        pdf.finishPage(page)
        FileOutputStream(file).use(pdf::writeTo)
        pdf.close()
        return file
    }

    fun exportWeeklyImage(
        context: Context,
        store: SleepStore,
        now: Long = System.currentTimeMillis()
    ): File {
        val file = File(context.cacheDir, "exports/nightpulse-weekly-report.png")
        file.parentFile?.mkdirs()

        val bitmap = Bitmap.createBitmap(1200, 1800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(244, 249, 252) }
        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(13, 43, 70) }
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(23, 191, 178) }
        val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(106, 139, 255) }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(70, 82, 101)
            textSize = 30f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(18, 52, 85)
            textSize = 36f
            isFakeBoldText = true
        }

        canvas.drawRect(0f, 0f, 1200f, 1800f, background)
        canvas.drawRoundRect(RectF(0f, 0f, 1200f, 240f), 0f, 0f, header)
        canvas.drawText("NightPulse Weekly Snapshot", 65f, 105f, titlePaint)
        canvas.drawText(formatDate(now), 65f, 160f, bodyPaint.apply { color = Color.rgb(218, 236, 245) })

        drawCard(canvas, white, 50f, 280f, 1100f, 400f)
        canvas.drawText("睡眠摘要", 80f, 340f, sectionPaint.apply { color = Color.rgb(18, 52, 85) })
        buildShareReport(store.activeSession ?: store.sessions.firstOrNull(), store.sessions, store.calibrationProfile, now)
            .lineSequence()
            .take(8)
            .forEachIndexed { index, line ->
                canvas.drawText(line, 80f, 415f + index * 46f, bodyPaint.apply { color = Color.rgb(70, 82, 101) })
            }

        drawCard(canvas, white, 50f, 730f, 1100f, 700f)
        canvas.drawText("最近趋势", 80f, 790f, sectionPaint)
        val weekly = buildWeeklyTrendPoints(store.sessions, now)
        val chartBaseY = 1260f
        weekly.forEachIndexed { index, point ->
            val left = 90f + index * 145f
            val barHeight = (point.durationHours * 75f).coerceAtMost(420f)
            canvas.drawRoundRect(RectF(left, chartBaseY - barHeight, left + 84f, chartBaseY), 18f, 18f, if (index % 2 == 0) accent else blue)
            canvas.drawText(point.label, left, chartBaseY + 45f, bodyPaint)
            canvas.drawText("${point.onsetMinutes}m", left, chartBaseY + 82f, bodyPaint)
        }

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        return file
    }

    private fun drawCard(canvas: Canvas, paint: Paint, x: Float, y: Float, width: Float, height: Float) {
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 34f, 34f, paint)
    }

    private fun formatDuration(durationMillis: Long): String {
        val safe = durationMillis.coerceAtLeast(0L)
        val totalSeconds = safe / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        return String.format("%02d:%02d", hours, minutes)
    }
}

object BackupExportManager {
    fun exportEncryptedBackup(
        context: Context,
        store: SleepStore
    ): File {
        val exportFile = File(context.cacheDir, "exports/nightpulse-encrypted-backup.npb")
        exportFile.parentFile?.mkdirs()

        val portableStore = store.toPortableStore()
        val json = exportJson.encodeToString(SleepStore.serializer(), portableStore)

        val zipBytes = ByteArrayOutputStream().use { byteStream ->
            ZipOutputStream(byteStream).use { zip ->
                zip.putNextEntry(ZipEntry("sleep-store.json"))
                zip.write(json.toByteArray())
                zip.closeEntry()

                store.sessions
                    .flatMap { session -> session.anomalyClips.map { session.id to it } }
                    .distinctBy { it.second.filePath }
                    .forEach { (sessionId, clip) ->
                        val file = File(clip.filePath)
                        if (file.exists()) {
                            val entryName = "clips/$sessionId/${file.name}"
                            zip.putNextEntry(ZipEntry(entryName))
                            zip.write(file.readBytes())
                            zip.closeEntry()
                        }
                    }
            }
            byteStream.toByteArray()
        }

        val encrypted = SecureStorage().encrypt(zipBytes)
        exportFile.writeBytes(encrypted)
        return exportFile
    }

    fun importEncryptedBackup(
        context: Context,
        uri: Uri
    ): SleepStore {
        val encrypted = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read backup file.")
        val zipBytes = SecureStorage().decrypt(encrypted)

        var parsedStore: SleepStore? = null
        val extractedFiles = mutableMapOf<String, String>()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory) {
                    if (name == "sleep-store.json") {
                        val jsonText = zip.readBytes().decodeToString()
                        parsedStore = exportJson.decodeFromString(SleepStore.serializer(), jsonText)
                    } else if (name.startsWith("clips/")) {
                        val target = File(context.filesDir, name)
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> output.write(zip.readBytes()) }
                        extractedFiles[name] = target.absolutePath
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val restored = parsedStore ?: error("Backup file is missing sleep-store.json")
        return restored.toRuntimeStore(extractedFiles)
    }

    fun toShareUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun SleepStore.toPortableStore(): SleepStore {
        fun remapSession(session: SleepSession): SleepSession {
            return session.copy(
                anomalyClips = session.anomalyClips.map { clip ->
                    val portablePath = File(clip.filePath).name.let { "clips/${session.id}/$it" }
                    clip.copy(filePath = portablePath)
                }
            )
        }

        return copy(
            activeSession = activeSession?.let(::remapSession),
            sessions = sessions.map(::remapSession)
        )
    }

    private fun SleepStore.toRuntimeStore(extractedFiles: Map<String, String>): SleepStore {
        fun remapSession(session: SleepSession): SleepSession {
            return session.copy(
                anomalyClips = session.anomalyClips.map { clip ->
                    val normalized = clip.filePath.replace('\\', '/')
                    val portable = if (normalized.startsWith("clips/")) {
                        normalized
                    } else {
                        "clips/${session.id}/${File(normalized).name}"
                    }
                    clip.copy(filePath = extractedFiles[portable] ?: clip.filePath)
                }
            )
        }

        val activeClosed = activeSession?.copy(endedAtMillis = activeSession.lastHeartbeatAtMillis)
        return copy(
            activeSession = null,
            activeNapPlan = null,
            activeSmartWakePlan = null,
            activeWhiteNoisePlan = null,
            activeAutoStopPlan = null,
            sessions = buildList {
                activeClosed?.let { add(remapSession(it)) }
                addAll(sessions.map(::remapSession))
            },
            bedtimePlan = bedtimePlan
        )
    }
}
