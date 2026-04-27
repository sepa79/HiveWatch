package io.pockethive.hivewatch.service.targetroles;

import io.pockethive.hivewatch.service.api.EnvironmentTargetRoleDto;
import io.pockethive.hivewatch.service.api.EnvironmentTargetRoleReplaceRequestDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentTargetRoleController {
    private final EnvironmentTargetRoleService service;

    public EnvironmentTargetRoleController(EnvironmentTargetRoleService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/environments/{environmentId}/target-roles")
    public List<EnvironmentTargetRoleDto> list(@PathVariable("environmentId") UUID environmentId) {
        return service.list(environmentId);
    }

    @PutMapping("/api/v1/admin/environments/{environmentId}/target-roles")
    public List<EnvironmentTargetRoleDto> replace(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody EnvironmentTargetRoleReplaceRequestDto request
    ) {
        return service.replace(environmentId, request);
    }
}
