package com.healthcheckassistant.ws1;

import com.healthcheckassistant.common.error.NotFoundException;
import com.healthcheckassistant.ws1.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Ws1Controller.class)
class Ws1ControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean Ws1Service service;
    private static final String BODY="{\"patientId\":\"P001\",\"stationId\":\"S002\"}";
    @Test void validBodyReturnsAllowedContract() throws Exception {
        when(service.check(new Ws1CheckRequest("P001","S002"))).thenReturn(new Ws1CheckResponse(true,"可以進行檢查","E002","胸部X光",List.of(new RemainingExamResponse("E001","血液檢查")),"test-token"));
        mvc.perform(post("/api/ws1/check").contentType("application/json").content(BODY)).andExpect(status().isOk()).andExpect(content().json("""
            {"allowed":true,"message":"可以進行檢查","examId":"E002","examName":"胸部X光","remainingExams":[{"examId":"E001","examName":"血液檢查"}],"code1":"test-token"}
            """,true));
    }
    @Test void businessDenialIs200WithNullCodeAndOmittedExam() throws Exception {
        when(service.check(new Ws1CheckRequest("P001","S002"))).thenReturn(new Ws1CheckResponse(false,"前置檢查尚未完成",null,null,List.of(),null));
        mvc.perform(post("/api/ws1/check").contentType("application/json").content(BODY)).andExpect(status().isOk()).andExpect(content().json("{\"allowed\":false,\"message\":\"前置檢查尚未完成\",\"remainingExams\":[],\"code1\":null}",true));
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"patientId\":\"\",\"stationId\":\"S002\"}","{\"patientId\":\"P001\"}","{\"patientId\":null,\"stationId\":\"S002\"}","{\"patientId\":\"  \",\"stationId\":\"S002\"}","{\"patientId\":\"P001\",\"stationId\":\"\"}","{", "[]", "null", "{\"patientId\":{},\"stationId\":\"S002\"}","{\"patientId\":123,\"stationId\":\"S002\"}","{\"patientId\":\"123456789012345678901\",\"stationId\":\"S002\"}"})
    void invalidRequestReturns400WithoutCallingService(String body) throws Exception {
        mvc.perform(post("/api/ws1/check").contentType("application/json").content(body)).andExpect(status().isBadRequest()).andExpect(content().json("{\"message\":\"請提供受檢者與站點代碼\"}",true));
        verifyNoInteractions(service);
    }
    @Test void absentBodyIs400() throws Exception {
        mvc.perform(post("/api/ws1/check").contentType("application/json")).andExpect(status().isBadRequest()).andExpect(content().json("{\"message\":\"請提供受檢者與站點代碼\"}",true)); verifyNoInteractions(service);
    }
    @Test void unknownPatientReturns404() throws Exception {
        when(service.check(new Ws1CheckRequest("P999","S002"))).thenThrow(new NotFoundException("找不到受檢者"));
        mvc.perform(post("/api/ws1/check").contentType("application/json").content("{\"patientId\":\"P999\",\"stationId\":\"S002\"}")).andExpect(status().isNotFound()).andExpect(content().json("{\"message\":\"找不到受檢者\"}",true));
    }
    @Test void unknownStationReturns404() throws Exception {
        when(service.check(new Ws1CheckRequest("P001","S999"))).thenThrow(new NotFoundException("找不到站點"));
        mvc.perform(post("/api/ws1/check").contentType("application/json").content("{\"patientId\":\"P001\",\"stationId\":\"S999\"}")).andExpect(status().isNotFound()).andExpect(content().json("{\"message\":\"找不到站點\"}",true));
    }
    @Test void configurationOrInsertErrorReturnsSafe500() throws Exception {
        when(service.check(new Ws1CheckRequest("P001","S002"))).thenThrow(new IllegalStateException("sensitive SQL DB username"));
        mvc.perform(post("/api/ws1/check").contentType("application/json").content(BODY)).andExpect(status().isInternalServerError()).andExpect(content().json("{\"message\":\"服務暫時無法使用\"}",true));
    }
}
