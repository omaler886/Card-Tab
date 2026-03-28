package com.codex.calorielens.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codex.calorielens.PrivacyPolicyActivity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CalorieLensApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context.findActivity()
    val application = context.applicationContext as Application
    val viewModel: MainViewModel = viewModel(factory = remember(application) {
        MainViewModelFactory(application)
    })
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val barcodeScanner = remember(activity) {
        activity?.let {
            GmsBarcodeScanning.getClient(
                it,
                GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128
                    )
                    .enableAutoZoom()
                    .build()
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.selectPhoto(pendingCameraUri?.let(Uri::parse))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.selectPhoto(uri)
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        viewModel.exportBackup(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.importBackup(uri)
    }

    LaunchedEffect(uiState.notice) {
        uiState.notice?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotice()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { HeaderCard() }
            item { SummaryCard(uiState) }
            item { HistoryChartsCard(uiState) }
            item { ProfileCard(uiState.profile, viewModel::saveProfile) }
            item {
                BarcodeDatabaseCard(
                    lookupState = uiState.lookup,
                    onSelectRegion = viewModel::setLookupRegion,
                    onScanBarcode = {
                        val scanner = barcodeScanner
                        if (scanner == null) {
                            viewModel.setLookupError("当前设备无法启动 Google 条码扫描器。")
                        } else {
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val value = barcode.rawValue ?: barcode.displayValue.orEmpty()
                                    viewModel.lookupBarcode(value)
                                }
                                .addOnFailureListener { error ->
                                    val message = error.localizedMessage?.takeIf { it.isNotBlank() }
                                        ?: "条码扫描已取消或失败。"
                                    viewModel.setLookupError(message)
                            }
                        }
                    },
                    onBarcodeDetected = viewModel::lookupBarcode,
                    onSearch = viewModel::searchFoodDatabase,
                    onAddItem = viewModel::addLookupItem
                )
            }
            item { ManualEntryCard(viewModel::addManualEntry) }
            item {
                PhotoRecognitionCard(
                    photoState = uiState.photo,
                    aiSettings = uiState.settings,
                    onTakePhoto = {
                        val uri = createTempImageUri(context)
                        pendingCameraUri = uri.toString()
                        cameraLauncher.launch(uri)
                    },
                    onPickGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAnalyze = viewModel::analyzeSelectedPhoto,
                    onAddRecognized = viewModel::addRecognizedItemsToToday
                )
            }
            item {
                RecipeSection(
                    uiState = uiState,
                    recipesForCuisine = viewModel::recipesFor,
                    recipeImpact = viewModel::recipeImpactText,
                    onAddRecipe = viewModel::addRecipe
                )
            }
            item { AiSettingsCard(uiState.settings, viewModel::saveAiSettings) }
            item {
                BackupRestoreCard(
                    backupState = uiState.backup,
                    onSavePreferences = viewModel::saveBackupPreferences,
                    onExportLocal = {
                        exportBackupLauncher.launch("calorielens-backup-${System.currentTimeMillis()}.json")
                    },
                    onImportLocal = {
                        importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    onUploadWebDav = viewModel::uploadWebDavBackup,
                    onRestoreWebDav = viewModel::restoreWebDavBackup,
                    onRegisterAccount = viewModel::registerSyncAccount,
                    onLoginAccount = viewModel::loginSyncAccount,
                    onRequestEmailVerification = viewModel::requestEmailVerification,
                    onVerifyEmail = viewModel::verifyEmail,
                    onRequestPasswordReset = viewModel::requestPasswordReset,
                    onResetPassword = viewModel::resetPassword,
                    onPushBackend = viewModel::pushBackendSync,
                    onForcePushBackend = viewModel::forcePushBackendSync,
                    onPullBackend = viewModel::pullBackendSync
                )
            }
            item {
                ReleaseReadyCard(
                    onOpenPrivacy = {
                        context.startActivity(Intent(context, PrivacyPolicyActivity::class.java))
                    }
                )
            }
            item { RecentEntriesCard(uiState.todayEntries, viewModel::deleteEntry) }
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
internal fun MetricPill(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.width(104.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun InfoBlock(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

internal fun createTempImageUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "captured").apply { mkdirs() }
    val imageFile = File(imageDir, "meal_${System.currentTimeMillis()}.jpg").apply {
        if (!exists()) {
            createNewFile()
        }
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

internal fun formatTime(timestamp: Long): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
