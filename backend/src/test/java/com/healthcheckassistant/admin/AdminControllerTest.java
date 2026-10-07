package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.AdminProgressResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminService service;
    @Test void returnsOrderedArrayWithExactlyFourFields() throws Exception {
        when(service.getProgress()).thenReturn(List.of(
                new AdminProgressResponse("P001", "測試受檢者A", 2, 5),
                new AdminProgressResponse("P003", "尚未安排", 0, 0)));
        mvc.perform(get("/api/admin/progress")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("""
                        [{"patientId":"P001","patientName":"測試受檢者A","completedCount":2,"totalCount":5},
                         {"patientId":"P003","patientName":"尚未安排","completedCount":0,"totalCount":0}]
                        """, true));
    }
    @Test void noPatientsReturns200EmptyArray() throws Exception {
        when(service.getProgress()).thenReturn(List.of());
        mvc.perform(get("/api/admin/progress")).andExpect(status().isOk()).andExpect(content().json("[]", true));
    }
    @Test void databaseFailureReturnsExistingGeneric500() throws Exception {
        when(service.getProgress()).thenThrow(new DataAccessResourceFailureException("private Oracle SQL details"));
        mvc.perform(get("/api/admin/progress")).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"message\":\"服務暫時無法使用\"}", true));
    }
}
