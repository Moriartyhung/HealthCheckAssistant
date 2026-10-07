package com.healthcheck.staff.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.healthcheck.staff.data.api.ApiConfig

/** 執行期修改 Backend Base URL（例：http://192.168.1.23:8080/）。 */
@Composable
fun ServerSettingsDialog(
    currentUrl: String,
    onSave: (String) -> String?,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(currentUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("伺服器設定") },
        text = {
            Column {
                Text(
                    "Spring Boot Backend 位址（Backend 電腦的 LAN IP）",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Text(
                    "Build 預設：${ApiConfig.defaultBaseUrl()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val err = onSave(text)
                if (err == null) onDismiss() else error = err
            }) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = { onReset(); onDismiss() }) { Text("還原預設") }
        },
    )
}
