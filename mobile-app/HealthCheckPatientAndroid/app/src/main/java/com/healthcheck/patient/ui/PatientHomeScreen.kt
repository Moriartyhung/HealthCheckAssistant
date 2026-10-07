package com.healthcheck.patient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthcheck.patient.data.model.ExamResponse
import com.healthcheck.patient.data.model.NextExamResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(vm: PatientViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val patientId by vm.patientId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("健檢進度") },
                navigationIcon = { TextButton(onClick = { vm.backToInput() }) { Text("返回") } },
                actions = {
                    val busy = state is PatientUiState.Loading ||
                        (state as? PatientUiState.Success)?.refreshing == true
                    TextButton(onClick = { vm.refresh() }, enabled = !busy) { Text("重新整理") }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                PatientUiState.Idle, PatientUiState.Loading -> CenterLoading()
                is PatientUiState.Error -> ErrorContent(
                    patientId = patientId.orEmpty(),
                    message = s.message,
                    onRetry = { vm.refresh() },
                    onBack = { vm.backToInput() },
                )
                is PatientUiState.Success -> SuccessContent(s)
            }
        }
    }
}

@Composable
private fun CenterLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("讀取中…")
        }
    }
}

@Composable
private fun ErrorContent(patientId: String, message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("查詢失敗", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text("Patient ID：$patientId", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("重試") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("重新輸入 Patient ID") }
    }
}

@Composable
private fun SuccessContent(s: PatientUiState.Success) {
    val progress = s.progress
    val exams = progress.exams.orEmpty()
    val completed = exams.filter { it.isCompleted }
    val pending = exams.filterNot { it.isCompleted }
    // 以 Backend 回傳的 completedCount / totalCount 為主；缺值時才以清單計算
    val total = progress.totalCount ?: exams.size
    val done = progress.completedCount ?: completed.size
    val ratio = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percent = if (total > 0) Math.round(ratio * 100) else 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (s.refreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(progress.patientName ?: "（未提供姓名）", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Patient ID：${progress.patientId ?: "-"}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$percent%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("已完成 $done / 共 $total 項", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(10.dp))
                }
            }
        }
        item { NextExamCard(s.next, s.nextError) }

        item { SectionTitle("未完成項目（${pending.size}）") }
        if (pending.isEmpty()) item { EmptyHint("沒有未完成的項目") }
        items(pending, key = { "p-" + (it.examId ?: it.hashCode().toString()) }) { ExamRow(it) }

        item { SectionTitle("已完成項目（${completed.size}）") }
        if (completed.isEmpty()) item { EmptyHint("尚無已完成的項目") }
        items(completed, key = { "c-" + (it.examId ?: it.hashCode().toString()) }) { ExamRow(it) }
    }
}

@Composable
private fun NextExamCard(next: NextExamResponse?, nextError: String?) {
    val exam = next?.nextExam
    val container = when {
        nextError != null -> MaterialTheme.colorScheme.errorContainer
        exam != null -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp)) {
            Text("下一站建議", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            when {
                nextError != null -> Text("無法取得下一站：$nextError", style = MaterialTheme.typography.bodyLarge)
                exam != null -> {
                    Text(exam.examName ?: exam.examId ?: "-", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("站點：${exam.stationName ?: "-"}（${exam.stationId ?: "-"}）", style = MaterialTheme.typography.titleMedium)
                    Text("開放時間：${exam.startTime ?: "--:--"} – ${exam.endTime ?: "--:--"}", style = MaterialTheme.typography.bodyLarge)
                    next.message?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // nextExam = null：全部完成或目前無可進行項目，直接顯示 Backend message
                else -> Text(
                    next?.message?.takeIf { it.isNotBlank() } ?: "目前沒有下一站建議",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ExamRow(exam: ExamResponse) {
    val statusColor: Color = if (exam.isCompleted) SuccessGreen else WarningOrange
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exam.examName ?: exam.examId ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${exam.stationName ?: "-"}（${exam.stationId ?: "-"}）", style = MaterialTheme.typography.bodyMedium)
                Text("開放時間 ${exam.startTime ?: "--:--"} – ${exam.endTime ?: "--:--"}", style = MaterialTheme.typography.bodySmall)
                if (exam.isCompleted && !exam.completedAt.isNullOrBlank()) {
                    Text("完成時間 ${exam.completedAt.replace('T', ' ')}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                if (exam.isCompleted) "已完成" else "未完成",
                color = statusColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
