package com.healthcheck.patient.data.api

import com.healthcheck.patient.data.repository.PatientRepository
import org.junit.Assert.*
import org.junit.Test

class ApiConfigTest {
    @Test fun rejectsUrlsWithoutAHostOrWithInvalidComponents() {
        listOf("", "http://?abc", "https://#fragment", "ftp://example.com/",
            "http://bad host/", "http://example.com:99999/", "https://user:secret@example.com/",
            "https://example.com/?query=1", "https://example.com/#fragment").forEach {
            assertNull("Should reject $it", ApiConfig.normalize(it))
        }
    }

    @Test fun normalizesValidHttpUrlsAndPreservesContextPath() {
        assertEquals("https://example.com/service/", ApiConfig.normalize("  https://example.com/service  "))
        assertEquals("http://example.com:8080/", ApiConfig.normalize("http://example.com:8080"))
        ApiConfig.createApi(ApiConfig.normalize("https://example.com/service")!!)
    }

    @Test fun failedApiCreationDoesNotReplaceRepositoryUrl() {
        val repository = PatientRepository("https://example.com/")
        try {
            repository.updateBaseUrl("http://?abc/")
            fail("Invalid URL should not be accepted")
        } catch (_: IllegalArgumentException) {
            assertEquals("https://example.com/", repository.baseUrl)
        }
    }
}
