package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetTemplateCreateRequestDto;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminExpectedSetTemplateController {
    private final ExpectedSetTemplateAdminService adminService;

    public AdminExpectedSetTemplateController(ExpectedSetTemplateAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/api/v1/admin/expected-set-templates")
    public ExpectedSetTemplateDto create(@RequestBody ExpectedSetTemplateCreateRequestDto request) {
        return adminService.create(request);
    }
}

