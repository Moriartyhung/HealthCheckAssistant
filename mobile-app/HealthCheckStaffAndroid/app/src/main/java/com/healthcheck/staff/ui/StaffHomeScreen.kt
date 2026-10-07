package com.healthcheck.staff.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(vm: StaffViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baseUrl by vm.baseUrl.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val busy = state.busy

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("健檢站點（工作人員）") },
                actions = { TextButton(onClick = { showSettings = true }, enabled = !busy) { Text("伺服器設定") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            IdInputRow(
                label = "Patient ID",
                placeholder = "例如 P001",
                value = state.patientId,
                error = state.patientIdError,
                scanText = "掃描受檢者",
                enabled = !busy,
                onValueChange = vm::onPatientIdChange,
                onScan = { vm.startScan(ScanTarget.PATIENT) },
            )
            Spacer(Modifier.height(12.dp))
            IdInputRow(
                label = "Station ID",
                placeholder = "例如 S002",
                value = state.stationId,
                error = state.stationIdError,
                scanText = "掃描檢查站",
                enabled = !busy,
                onValueChange = vm::onStationIdChange,
                onScan = { vm.startScan(ScanTarget.STATION) },
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = vm::checkEligibility,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("檢查資格", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(16.dp))

            CheckResultSection(
                state = state,
                onConfirmComplete = vm::confirmComplete,
                onNext = vm::resetAll,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "伺服器：$baseUrl",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSettings) {
        ServerSettingsDialog(
            currentUrl = baseUrl,
            onSave = { vm.updateBaseUrl(it) },
            onReset = { vm.resetBaseUrl() },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun IdInputRow(
    label: String,
    placeholder: String,
    value: String,
    error: String?,
    scanText: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onScan: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            enabled = enabled,
            isError = error != null,
            supportingText = { error?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Next,
            ),
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onScan,
            enabled = enabled,
            modifier = Modifier
                .padding(top = 8.dp)
                .height(56.dp),
        ) { Text(scanText) }
    }
}
