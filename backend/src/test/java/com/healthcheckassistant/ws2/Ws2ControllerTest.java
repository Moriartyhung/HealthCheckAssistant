package com.healthcheckassistant.ws2;

import com.healthcheckassistant.common.error.*;
import com.healthcheckassistant.ws2.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Ws2Controller.class)
class Ws2ControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean Ws2Service service;
    private static final String BODY="{\"code1\":\"codeA\"}";
    @Test void successReturnsFrontendContract() throws Exception {
        when(service.complete(new Ws2CompleteRequest("codeA"))).thenReturn(new Ws2CompleteResponse(true,"檢查完成狀態已更新"));
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content(BODY)).andExpect(status().isOk()).andExpect(content().json("{\"success\":true,\"message\":\"檢查完成狀態已更新\"}",true));
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"code1\":\"\"}","{\"code1\":\"  \"}","{\"code1\":null}","{\"code1\":123}","{\"code1\":true}","{\"code1\":{}}","{\"code1\":[]}","{","[]","null",""})
    void invalidRequestReturns400WithoutService(String body) throws Exception {
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content(body)).andExpect(status().isBadRequest()).andExpect(content().json("{\"message\":\"請提供 code1\"}",true)); verifyNoInteractions(service);
    }
    @Test void oversizedTokenIs400() throws Exception {
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content("{\"code1\":\""+"a".repeat(101)+"\"}")).andExpect(status().isBadRequest()).andExpect(content().json("{\"message\":\"請提供 code1\"}",true)); verifyNoInteractions(service);
    }
    @Test void contextPathKeepsWs2ValidationMessage() throws Exception {
        mvc.perform(post("/app/api/ws2/complete").contextPath("/app").contentType("application/json").content("{}")).andExpect(status().isBadRequest()).andExpect(content().json("{\"message\":\"請提供 code1\"}",true)); verifyNoInteractions(service);
    }
    @Test void invalidTokenIs404() throws Exception {
        when(service.complete(new Ws2CompleteRequest("codeA"))).thenThrow(new NotFoundException("無效的確認碼，請重新判斷"));
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content(BODY)).andExpect(status().isNotFound()).andExpect(content().json("{\"message\":\"無效的確認碼，請重新判斷\"}",true));
    }
    @ParameterizedTest @ValueSource(strings={"確認碼已使用，請勿重複提交","此檢查已完成"})
    void conflictsHave409Message(String message) throws Exception {
        when(service.complete(new Ws2CompleteRequest("codeA"))).thenThrow(new ConflictException(message));
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content(BODY)).andExpect(status().isConflict()).andExpect(content().json("{\"message\":\""+message+"\"}",true));
    }
    @Test void databaseDetailsAreNotExposed() throws Exception {
        when(service.complete(new Ws2CompleteRequest("codeA"))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Oracle connection SQL credential details"));
        mvc.perform(post("/api/ws2/complete").contentType("application/json").content(BODY)).andExpect(status().isInternalServerError()).andExpect(content().json("{\"message\":\"服務暫時無法使用\"}",true));
    }
}
