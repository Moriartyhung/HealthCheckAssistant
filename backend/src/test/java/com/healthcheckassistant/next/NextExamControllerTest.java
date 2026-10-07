package com.healthcheckassistant.next;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.next.dto.NextExamResponse;
import com.healthcheckassistant.patient.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NextExamController.class)
class NextExamControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean NextExamService service;
    @Test void recommendedExamHasCompleteContract() throws Exception {
        when(service.getNext("P001")).thenReturn(new NextExamResponse(
                new ExamResponse("E001", "血液檢查", "S001", "抽血站", "08:00", "11:00", ExamStatus.PENDING, null),
                "建議前往以下站點"));
        mvc.perform(get("/api/patients/P001/next")).andExpect(status().isOk()).andExpect(content().json("""
                {"nextExam":{"examId":"E001","examName":"血液檢查","stationId":"S001","stationName":"抽血站",
                "startTime":"08:00","endTime":"11:00","status":"PENDING","completedAt":null},"message":"建議前往以下站點"}
                """, true));
    }
    @Test void completedResponseKeepsExplicitNull() throws Exception {
        when(service.getNext("P001")).thenReturn(new NextExamResponse(null,"所有檢查已完成，謝謝您的配合！"));
        mvc.perform(get("/api/patients/P001/next")).andExpect(status().isOk())
                .andExpect(content().json("{\"nextExam\":null,\"message\":\"所有檢查已完成，謝謝您的配合！\"}",true));
    }
    @Test void unavailableResponseIs200WithExplicitNull() throws Exception {
        when(service.getNext("P001")).thenReturn(new NextExamResponse(null,"目前沒有可進行的檢查，請洽工作人員"));
        mvc.perform(get("/api/patients/P001/next")).andExpect(status().isOk())
                .andExpect(content().json("{\"nextExam\":null,\"message\":\"目前沒有可進行的檢查，請洽工作人員\"}",true));
    }
    @Test void unknownPatientReturns404() throws Exception {
        when(service.getNext("P999")).thenThrow(new NotFoundException("找不到受檢者"));
        mvc.perform(get("/api/patients/P999/next")).andExpect(status().isNotFound())
                .andExpect(content().json("{\"message\":\"找不到受檢者\"}",true));
    }
    @Test void invalidPatientIdRejectedBeforeService() throws Exception {
        mvc.perform(get("/api/patients/{patientId}/next"," ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/patients/123456789012345678901/next")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void hidesDatabaseFailureDetails() throws Exception {
        when(service.getNext("P001")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private database detail"));
        mvc.perform(get("/api/patients/P001/next")).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"message\":\"服務暫時無法使用\"}",true));
    }
}
