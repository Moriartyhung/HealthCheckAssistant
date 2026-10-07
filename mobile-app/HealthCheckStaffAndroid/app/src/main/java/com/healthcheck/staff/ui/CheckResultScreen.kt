package com.healthcheck.staff.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** WS1 / WS2 結果區塊（嵌在 StaffHomeScreen 下半部）。 */
@Composable
fun CheckResultSection(
    state: StaffUiState,
    onConfirmComplete: () -> Unit,
    onNext: () -> Unit,
) {
    when (val ws1 = state.ws1) {
        Ws1State.Idle -> Unit
        Ws1State.Loading -> LoadingRow("檢查資格中…")
        is Ws1State.Error -> ResultCard(
            title = "無法完成資格檢查",
            body = ws1.message,
            container = MaterialTheme.colorScheme.errorContainer,
        )
        is Ws1State.Result -> Ws1ResultCard(ws1, state, onConfirmComplete)
    }

    when (val ws2 = state.ws2) {
        Ws2State.Idle -> Unit
        Ws2State.Loading -> LoadingRow("送出完成中…")
        is Ws2State.Result -> {
            val ok = ws2.response.success == true
            ResultCard(
                title = if (ok) "✔ 檢查完成" else "未完成",
                body = ws2.response.message ?: if (ok) "檢查完成狀態已更新" else "Backend 回傳 success=false",
                container = if (ok) Color(0xFFC8E6C9) else MaterialTheme.colorScheme.errorContainer,
            )
        }
        is Ws2State.Error -> ResultCard(
            title = when (ws2.code) {
                409 -> "確認碼已使用或此檢查已完成"
                404 -> "無效的確認碼"
                else -> "完成失敗"
            },
            body = ws2.message,
            container = MaterialTheme.colorScheme.errorContainer,
        )
    }

    if (state.ws1 is Ws1State.Result || state.ws2 !is Ws2State.Idle) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onNext, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("下一位受檢者（清除）")
        }
    }
}

@Composable
private fun Ws1ResultCard(ws1: Ws1State.Result, state: StaffUiState, onConfirmComplete: () -> Unit) {
    val r = ws1.response
    val allowed = r.allowed == true
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allowed) Color(0xFFC8E6C9) else Color(0xFFFFE0B2),
            contentColor = Color(0xFF1B1B1B),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (allowed) "✔ 可進行檢查" else "✖ 目前不可進行此檢查",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (allowed) SuccessGreen else WarningOrange,
            )
            Spacer(Modifier.height(4.dp))
            Text("Patient：${ws1.patientId}　Station：${ws1.stationId}", style = MaterialTheme.typography.bodyMedium)
            r.message?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
            if (r.examName != null || r.examId != null) {
                Spacer(Modifier.height(4.dp))
                Text("檢查項目：${r.examName ?: "-"}（${r.examId ?: "-"}）", style = MaterialTheme.typography.bodyLarge)
            }
            val remaining = r.remainingExams.orEmpty()
            if (remaining.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("尚未完成的項目：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                remaining.forEach {
                    Text("• ${it.examName ?: "-"}（${it.examId ?: "-"}）", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (allowed) {
                Spacer(Modifier.height(8.dp))
                val code = state.code1
                when {
                    code != null -> {
                        Text("確認碼 code1：$code", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onConfirmComplete,
                            enabled = state.ws2 !is Ws2State.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text("確認完成", style = MaterialTheme.typography.titleMedium) }
                    }
                    state.ws2 is Ws2State.Idle -> Text(
                        "⚠ Backend 回傳 allowed=true 但沒有 code1，無法送出完成",
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Unit // code1 已被 WS2 消耗
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ResultCard(title: String, body: String, container: Color) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun LoadingRow(text: String) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
