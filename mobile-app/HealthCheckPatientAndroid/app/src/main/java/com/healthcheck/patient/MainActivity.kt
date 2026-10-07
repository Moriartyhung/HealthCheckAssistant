package com.healthcheck.patient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.healthcheck.patient.ui.HealthCheckTheme
import com.healthcheck.patient.ui.PatientHomeScreen
import com.healthcheck.patient.ui.PatientIdScreen
import com.healthcheck.patient.ui.PatientViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthCheckTheme {
                val vm: PatientViewModel = viewModel()
                val patientId by vm.patientId.collectAsStateWithLifecycle()
                if (patientId == null) {
                    PatientIdScreen(vm)
                } else {
                    BackHandler { vm.backToInput() }
                    PatientHomeScreen(vm)
                }
            }
        }
    }
}
