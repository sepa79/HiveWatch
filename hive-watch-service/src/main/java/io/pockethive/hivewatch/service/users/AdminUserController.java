package io.pockethive.hivewatch.service.users;

import io.pockethive.hivewatch.service.api.UserCreateRequestDto;
import io.pockethive.hivewatch.service.api.UserEnvironmentVisibilityUpdateRequestDto;
import io.pockethive.hivewatch.service.api.UserSummaryDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {
    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/api/v1/admin/users")
    public List<UserSummaryDto> list() {
        return userAdminService.listUsers();
    }

    @PostMapping("/api/v1/admin/users")
    public UserSummaryDto create(@RequestBody UserCreateRequestDto request) {
        return userAdminService.createUser(request);
    }

    @GetMapping("/api/v1/admin/users/{userId}/environment-visibility")
    public UserEnvironmentVisibilityUpdateRequestDto getVisibility(@PathVariable("userId") UUID userId) {
        return userAdminService.getVisibility(userId);
    }

    @PutMapping("/api/v1/admin/users/{userId}/environment-visibility")
    public UserEnvironmentVisibilityUpdateRequestDto replaceVisibility(
            @PathVariable("userId") UUID userId,
            @RequestBody UserEnvironmentVisibilityUpdateRequestDto request
    ) {
        return userAdminService.replaceVisibility(userId, request);
    }
}

