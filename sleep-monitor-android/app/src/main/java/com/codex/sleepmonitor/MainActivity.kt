package com.codex.sleepmonitor

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.sleepmonitor.data.BackupExportManager
import com.codex.sleepmonitor.data.ReportExportManager
import com.codex.sleepmonitor.data.SoothingSoundType
import com.codex.sleepmonitor.service.BedtimeScheduler
import com.codex.sleepmonitor.service.SleepMonitoringService
import com.codex.sleepmonitor.ui.buildShareReport
import com.codex.sleepmonitor.ui.SleepMonitorScreen
import com.codex.sleepmonitor.ui.SleepMonitorViewModel
import com.codex.sleepmonitor.ui.theme.SleepMonitorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as SleepMonitorApp
            val viewModel: SleepMonitorViewModel = viewModel(
                factory = SleepMonitorViewModel.Factory(app.repository)
            )
            val store by viewModel.store.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val activity = remember(context) { context.findActivity() }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { grants ->
                val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                if (audioGranted) {
                    SleepMonitoringService.start(context)
                }
            }
            val importBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    activity?.let { host ->
                        host.lifecycleScope.launch(Dispatchers.IO) {
                            runCatching {
                                val restored = BackupExportManager.importEncryptedBackup(host, uri)
                                app.repository.replaceStore(restored)
                            }.onSuccess {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(host, "备份已恢复", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(host, "恢复失败: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }

            SleepMonitorTheme {
                SleepMonitorScreen(
                    store = store,
                    onStartRequested = {
                        val missing = buildList {
                            if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                add(Manifest.permission.RECORD_AUDIO)
                            }

                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        if (missing.isEmpty()) {
                            SleepMonitoringService.start(context)
                        } else {
                            launcher.launch(missing.toTypedArray())
                        }
                    },
                    onStopRequested = { SleepMonitoringService.stop(context) },
                    onStartNapRequested = { minutes ->
                        SleepMonitoringService.startNap(context, minutes)
                    },
                    onCancelNapRequested = { SleepMonitoringService.cancelNap(context) },
                    onStartSmartWakeRequested = { hours, windowMinutes ->
                        SleepMonitoringService.startSmartWake(context, hours, windowMinutes)
                    },
                    onCancelSmartWakeRequested = { SleepMonitoringService.cancelSmartWake(context) },
                    onStartWhiteNoiseRequested = { soundType, volume, durationMinutes ->
                        SleepMonitoringService.startWhiteNoise(context, soundType, volume, durationMinutes)
                    },
                    onStopWhiteNoiseRequested = { SleepMonitoringService.stopWhiteNoise(context) },
                    onStartAutoStopRequested = { hours ->
                        SleepMonitoringService.startAutoStop(context, hours)
                    },
                    onCancelAutoStopRequested = { SleepMonitoringService.cancelAutoStop(context) },
                    onSetBedtimePlanRequested = { hour, minute, autoStart ->
                        val next = BedtimeScheduler.computeNextTrigger(hour, minute)
                        viewModel.setBedtimePlan(hour, minute, autoStart, next)
                        BedtimeScheduler.schedule(
                            context,
                            com.codex.sleepmonitor.data.BedtimePlan(
                                hour = hour,
                                minute = minute,
                                autoStart = autoStart,
                                nextTriggerAtMillis = next
                            )
                        )
                    },
                    onClearBedtimePlanRequested = {
                        viewModel.clearBedtimePlan()
                        BedtimeScheduler.cancel(context)
                    },
                    onStopAlarmRequested = { SleepMonitoringService.stopAlarm(context) },
                    onCaffeineChanged = viewModel::updateCaffeine,
                    onStressChanged = viewModel::updateStress,
                    onExerciseChanged = viewModel::updateExercise,
                    onLateMealToggled = viewModel::toggleLateMeal,
                    onAlcoholToggled = viewModel::toggleAlcohol,
                    onRecalibrateRequested = viewModel::recalibrate,
                    onShareReportRequested = {
                        val report = buildShareReport(
                            focusSession = store.activeSession ?: store.sessions.firstOrNull(),
                            sessions = store.sessions,
                            calibrationProfile = store.calibrationProfile,
                            now = System.currentTimeMillis()
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report)
                            putExtra(Intent.EXTRA_SUBJECT, "NightPulse 睡眠报告")
                        }
                        activity?.startActivity(Intent.createChooser(intent, "分享睡眠报告"))
                    },
                    onExportWeeklyPdfRequested = {
                        activity?.let { host ->
                            host.lifecycleScope.launch(Dispatchers.IO) {
                                val file = ReportExportManager.exportWeeklyPdf(host, store)
                                val uri = BackupExportManager.toShareUri(host, file)
                                withContext(Dispatchers.Main) {
                                    host.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "NightPulse 周报 PDF")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            "分享周报 PDF"
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onExportWeeklyImageRequested = {
                        activity?.let { host ->
                            host.lifecycleScope.launch(Dispatchers.IO) {
                                val file = ReportExportManager.exportWeeklyImage(host, store)
                                val uri = BackupExportManager.toShareUri(host, file)
                                withContext(Dispatchers.Main) {
                                    host.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "NightPulse 周报图片")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            "分享周报图片"
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onExportEncryptedBackupRequested = {
                        activity?.let { host ->
                            host.lifecycleScope.launch(Dispatchers.IO) {
                                val file = BackupExportManager.exportEncryptedBackup(host, store)
                                val uri = BackupExportManager.toShareUri(host, file)
                                withContext(Dispatchers.Main) {
                                    host.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "application/octet-stream"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "NightPulse 加密备份")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            "备份到云盘/网盘"
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onImportEncryptedBackupRequested = {
                        importBackupLauncher.launch(arrayOf("*/*"))
                    },
                    onOpenSettings = { activity?.startActivity(it) }
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
