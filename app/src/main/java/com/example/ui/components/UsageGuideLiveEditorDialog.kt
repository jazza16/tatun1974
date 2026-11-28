package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.auth.AuthManager
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.AppUsageGuideRepository
import com.example.model.AppUsageGuide
import com.example.model.DefaultAppUsageGuideCatalog
import com.example.model.UsageGuideStep
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

/**
 * Dialog khusus Administrator BPBD untuk menyunting dan menerbitkan Panduan Penggunaan
 * ke Cloud Firestore (`app_settings/usage_guide`) tanpa perlu membangun ulang/update APK.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageGuideLiveEditorDialog(
    initialGuide: AppUsageGuide,
    onDismiss: () -> Unit,
    onPublished: (AppUsageGuide) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val officerAuthManager = remember { OfficerAuthManager(context) }
    val authManager = remember { AuthManager(context) }

    var editingRoleIsOfficer by remember { mutableStateOf(false) }
    var selectedStepIndex by remember { mutableIntStateOf(0) }

    var guideVersion by remember { mutableStateOf(initialGuide.guideVersion) }
    var updatedAtLabel by remember { mutableStateOf(initialGuide.updatedAtLabel) }
    var citizenSummary by remember { mutableStateOf(initialGuide.citizenSummary) }
    var officerSummary by remember { mutableStateOf(initialGuide.officerSummary) }
    var citizenSteps by remember { mutableStateOf(initialGuide.citizenSteps) }
    var officerSteps by remember { mutableStateOf(initialGuide.officerSteps) }

    var firebaseUserEmail by remember {
        mutableStateOf(
            try { FirebaseAuth.getInstance().currentUser?.email } catch (_: Exception) { null }
        )
    }
    var isFirebaseSignedIn by remember {
        mutableStateOf(
            try { FirebaseAuth.getInstance().currentUser != null } catch (_: Exception) { false }
        )
    }
    var isSigningInGoogle by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val activeSteps = if (editingRoleIsOfficer) officerSteps else citizenSteps
    val safeStepIdx = selectedStepIndex.coerceIn(0, (activeSteps.size - 1).coerceAtLeast(0))
    val currentStep = activeSteps.getOrNull(safeStepIdx) ?: UsageGuideStep(1, "", "", "Umum")

    fun updateCurrentStep(updated: UsageGuideStep) {
        if (editingRoleIsOfficer) {
            officerSteps = officerSteps.mapIndexed { idx, item ->
                if (idx == safeStepIdx) updated else item
            }
        } else {
            citizenSteps = citizenSteps.mapIndexed { idx, item ->
                if (idx == safeStepIdx) updated else item
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 580.dp)
                .testTag("usage_guide_admin_editor_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sunting Panduan Live Cloud",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Perubahan langsung tampil di seluruh perangkat tanpa update APK.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Editor"
                        )
                    }
                }

                // Auth Status Check for Cloud Write
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFirebaseSignedIn) {
                            Color(0xFF16A34A).copy(alpha = 0.1f)
                        } else {
                            OrangePrimary.copy(alpha = 0.1f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isFirebaseSignedIn) Color(0xFF16A34A).copy(alpha = 0.35f)
                        else OrangePrimary.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isFirebaseSignedIn) Color(0xFF15803D) else OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFirebaseSignedIn) {
                                    "Otorisasi Cloud Aktif (${firebaseUserEmail ?: "Akun Google Admin"})"
                                } else {
                                    "Masuk Google Admin diperlukan untuk menerbitkan ke Firestore Cloud."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!isFirebaseSignedIn) {
                            OutlinedButton(
                                onClick = {
                                    val activity = context.findActivityOrNull()
                                    if (activity == null) {
                                        isError = true
                                        feedbackMessage = "Konteks layar tidak mendukung Google Sign-In."
                                        return@OutlinedButton
                                    }
                                    isSigningInGoogle = true
                                    feedbackMessage = null
                                    coroutineScope.launch {
                                        val res = authManager.signInWithGoogle(activity)
                                        isSigningInGoogle = false
                                        res.onSuccess { user ->
                                            isFirebaseSignedIn = true
                                            firebaseUserEmail = user.email
                                            isError = false
                                            feedbackMessage = "Akun Google Admin terhubung. Siap menerbitkan ke Cloud."
                                        }.onFailure { err ->
                                            isError = true
                                            feedbackMessage = err.localizedMessage ?: "Gagal masuk Google Admin."
                                        }
                                    }
                                },
                                enabled = !isSigningInGoogle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_admin_google_signin_guide"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isSigningInGoogle) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isSigningInGoogle) "Menghubungkan Google..." else "Masuk dengan Google Admin",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Version & Date metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = guideVersion,
                        onValueChange = { guideVersion = it.take(64) },
                        label = { Text("Versi Panduan", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = updatedAtLabel,
                        onValueChange = { updatedAtLabel = it.take(80) },
                        label = { Text("Tanggal Pembaruan", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Role switch for editing
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !editingRoleIsOfficer,
                        onClick = {
                            editingRoleIsOfficer = false
                            selectedStepIndex = 0
                        },
                        label = { Text("Panduan Warga", maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = editingRoleIsOfficer,
                        onClick = {
                            editingRoleIsOfficer = true
                            selectedStepIndex = 0
                        },
                        label = { Text("Panduan Petugas", maxLines = 1, softWrap = false) }
                    )
                }

                OutlinedTextField(
                    value = if (editingRoleIsOfficer) officerSummary else citizenSummary,
                    onValueChange = { value ->
                        if (editingRoleIsOfficer) officerSummary = value.take(500)
                        else citizenSummary = value.take(500)
                    },
                    label = {
                        Text(
                            if (editingRoleIsOfficer) "Ringkasan Mode Petugas" else "Ringkasan Mode Warga",
                            fontSize = 11.sp
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Step Selector Pills
                Text(
                    text = "Pilih Langkah yang Disunting:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeSteps.forEachIndexed { idx, _ ->
                        FilterChip(
                            selected = safeStepIdx == idx,
                            onClick = { selectedStepIndex = idx },
                            label = { Text("Langkah ${idx + 1}", maxLines = 1, softWrap = false) }
                        )
                    }
                }

                OutlinedTextField(
                    value = currentStep.moduleTag,
                    onValueChange = { updateCurrentStep(currentStep.copy(moduleTag = it.take(60))) },
                    label = { Text("Label Modul / Tab", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentStep.title,
                    onValueChange = { updateCurrentStep(currentStep.copy(title = it.take(120))) },
                    label = { Text("Judul Langkah ${safeStepIdx + 1}", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentStep.description,
                    onValueChange = { updateCurrentStep(currentStep.copy(description = it.take(600))) },
                    label = { Text("Isi Panduan Ringkas", fontSize = 11.sp) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!feedbackMessage.isNullOrBlank()) {
                    Text(
                        text = feedbackMessage.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                    )
                }

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val defaults = DefaultAppUsageGuideCatalog.defaultGuide()
                            guideVersion = defaults.guideVersion
                            updatedAtLabel = defaults.updatedAtLabel
                            citizenSummary = defaults.citizenSummary
                            officerSummary = defaults.officerSummary
                            citizenSteps = defaults.citizenSteps
                            officerSteps = defaults.officerSteps
                            selectedStepIndex = 0
                            isError = false
                            feedbackMessage = "Isi dikembalikan ke standar v1.3.1 (belum diterbitkan)."
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Standar", fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            isPublishing = true
                            feedbackMessage = null
                            val adminName = officerAuthManager.getOfficerName().ifBlank {
                                firebaseUserEmail ?: "Admin BPBD Mukomuko"
                            }
                            val updatedGuide = initialGuide.copy(
                                guideVersion = guideVersion,
                                updatedAtLabel = updatedAtLabel,
                                updatedBy = adminName,
                                citizenSummary = citizenSummary,
                                officerSummary = officerSummary,
                                citizenSteps = citizenSteps,
                                officerSteps = officerSteps
                            )
                            coroutineScope.launch {
                                val res = AppUsageGuideRepository.publishGuideToCloud(context, updatedGuide)
                                isPublishing = false
                                res.onSuccess { published ->
                                    onPublished(published)
                                }.onFailure { err ->
                                    isError = true
                                    feedbackMessage = err.localizedMessage ?: "Gagal menerbitkan ke Firestore."
                                }
                            }
                        },
                        enabled = !isPublishing && isFirebaseSignedIn,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_publish_usage_guide_cloud"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = OrangeOnPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = OrangeOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isPublishing) "Menerbitkan..." else "Terbitkan Cloud",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeOnPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
