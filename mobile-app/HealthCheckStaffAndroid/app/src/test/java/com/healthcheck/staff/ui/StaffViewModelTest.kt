package com.healthcheck.staff.ui

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import com.healthcheck.staff.data.api.ApiConfig
import com.healthcheck.staff.data.api.HealthCheckApi
import com.healthcheck.staff.data.model.*
import com.healthcheck.staff.data.repository.StaffRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {
    private val store = ViewModelStore()
    private lateinit var vm: StaffViewModel
    private val api = DelayedApi()
    private var savedUrl: String? = null

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = mock(Application::class.java)
        val prefs = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(app.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.getString(anyString(), isNull())).thenAnswer { savedUrl }
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenAnswer {
            savedUrl = it.getArgument(1); editor
        }
        `when`(editor.remove(anyString())).thenAnswer { savedUrl = null; editor }
        vm = StaffViewModel(app)
        store.put("staff", vm)
        // Replace only the external API boundary. ViewModel, Repository and safeApiCall stay real.
        val repository = StaffViewModel::class.java.getDeclaredField("repository").apply { isAccessible = true }.get(vm)
        StaffRepository::class.java.getDeclaredField("api").apply { isAccessible = true }.set(repository, api)
        vm.onPatientIdChange("P001")
        vm.onStationIdChange("S002")
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun ws1InFlightBlocksResetInputScanningAndServerChange() {
        vm.checkEligibility()
        assertTrue(vm.state.value.ws1 is Ws1State.Loading)
        assertBusyFlowIsProtected()
    }

    @Test fun ws2InFlightCannotBeClearedAndConsumesCodeOnlyAfterSuccess() {
        vm.checkEligibility()
        api.check.complete(Response.success(Ws1CheckResponse(allowed = true, code1 = "confirmation")))
        assertEquals("confirmation", vm.state.value.code1)
        vm.confirmComplete()
        assertTrue(vm.state.value.ws2 is Ws2State.Loading)
        assertBusyFlowIsProtected()
        api.complete.complete(Response.success(Ws2CompleteResponse(success = true)))
        assertNull(vm.state.value.code1)
        assertTrue(vm.state.value.ws2 is Ws2State.Result)
        vm.resetAll()
        assertEquals(StaffUiState(), vm.state.value)
    }

    private fun assertBusyFlowIsProtected() {
        val before = vm.state.value
        val url = vm.baseUrl.value
        vm.resetAll()
        vm.onPatientIdChange("P002")
        vm.onStationIdChange("S003")
        vm.startScan(ScanTarget.PATIENT)
        assertNotNull(vm.updateBaseUrl("https://other.example/"))
        vm.resetBaseUrl()
        assertEquals(before, vm.state.value)
        assertEquals(url, vm.baseUrl.value)
        assertNull(savedUrl)
    }

    @Test fun changingServerWhileIdleInvalidatesExistingConfirmation() {
        vm.checkEligibility()
        api.check.complete(Response.success(Ws1CheckResponse(allowed = true, code1 = "confirmation")))
        assertNull(vm.updateBaseUrl("https://other.example/service"))
        assertNull(vm.state.value.code1)
        assertEquals(Ws1State.Idle, vm.state.value.ws1)
        assertEquals("https://other.example/service/", savedUrl)
    }

    @Test fun invalidServerDoesNotCrashOrPersistOrChangeCurrentState() {
        val before = vm.state.value
        val url = vm.baseUrl.value
        assertNotNull(vm.updateBaseUrl("http://?abc"))
        assertNull(savedUrl)
        assertEquals(url, vm.baseUrl.value)
        assertEquals(before, vm.state.value)
    }

    @Test fun rejectsInvalidHostAndAcceptsContextPath() {
        assertNull(ApiConfig.normalize("http://?abc"))
        assertNull(ApiConfig.normalize("ftp://example.com/"))
        assertEquals("https://example.com/service/", ApiConfig.normalize("https://example.com/service"))
    }

    @Test fun patientScannerRejectsStationQrAndAcceptsPatientQr() {
        vm.startScan(ScanTarget.PATIENT)
        assertFalse(vm.onQrScanned("S002"))
        assertEquals(ScanTarget.PATIENT, vm.state.value.scanTarget)
        assertNotNull(vm.state.value.scanMessage)
        assertTrue(vm.onQrScanned(" P001 "))
        assertEquals("P001", vm.state.value.patientId)
        assertNull(vm.state.value.scanTarget)
    }

    @Test fun stationScannerRejectsPatientQrAndAcceptsStationQr() {
        vm.startScan(ScanTarget.STATION)
        assertFalse(vm.onQrScanned("P001"))
        assertTrue(vm.onQrScanned("S002"))
        assertEquals("S002", vm.state.value.stationId)
    }

    @Test fun scannerRejectsEmptyNonIdAndOversizedContent() {
        vm.startScan(ScanTarget.PATIENT)
        listOf("", "https://example.com/", "{\"id\":\"P001\"}", "P" + "1".repeat(20)).forEach {
            assertFalse(vm.onQrScanned(it))
        }
    }

    private class DelayedApi : HealthCheckApi {
        val check = CompletableDeferred<Response<Ws1CheckResponse>>()
        val complete = CompletableDeferred<Response<Ws2CompleteResponse>>()
        override suspend fun ws1Check(body: Ws1CheckRequest) = check.await()
        override suspend fun ws2Complete(body: Ws2CompleteRequest) = complete.await()
    }
}
