package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.AdminProgressResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {
    @Test void returnsRepositoryProgressUnchanged() {
        var repository = mock(AdminRepository.class);
        var progress = List.of(new AdminProgressResponse("P001", "測試受檢者A", 2, 5));
        when(repository.findAllProgress()).thenReturn(progress);
        assertThat(new AdminService(repository).getProgress()).isSameAs(progress);
        verify(repository).findAllProgress();
        verifyNoMoreInteractions(repository);
    }
    @Test void emptyRepositoryReturnsEmptyList() {
        var repository = mock(AdminRepository.class);
        when(repository.findAllProgress()).thenReturn(List.of());
        assertThat(new AdminService(repository).getProgress()).isEmpty();
    }
}
