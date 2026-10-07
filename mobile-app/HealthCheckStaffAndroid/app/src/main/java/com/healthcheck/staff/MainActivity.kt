package com.healthcheck.staff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthcheck.staff.ui.HealthCheckTheme
import com.healthcheck.staff.ui.ScannerScreen
import com.healthcheck.staff.ui.StaffHomeScreen
import com.healthcheck.staff.ui.StaffViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthCheckTheme {
                val vm: StaffViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                val target = state.scanTarget
                if (target != null) {
                    ScannerScreen(
                        target = target,
                        message = state.scanMessage,
                        onScanned = vm::onQrScanned,
                        onError = vm::onScanMessage,
                        onCancel = vm::cancelScan,
                    )
                } else {
                    StaffHomeScreen(vm)
                }
            }
        }
    }
}
