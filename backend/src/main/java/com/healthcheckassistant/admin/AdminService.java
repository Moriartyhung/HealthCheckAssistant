package com.healthcheckassistant.admin;

import com.healthcheckassistant.admin.dto.AdminProgressResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AdminService {
    private final AdminRepository repository;

    public AdminService(AdminRepository repository) {
        this.repository = repository;
    }

    public List<AdminProgressResponse> getProgress() {
        return repository.findAllProgress();
    }
}
