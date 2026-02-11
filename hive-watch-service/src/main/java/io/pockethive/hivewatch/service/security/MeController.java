package io.pockethive.hivewatch.service.security;

import io.pockethive.hivewatch.service.api.UserSummaryDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/v1/me")
    public UserSummaryDto me() {
        HiveWatchPrincipal p = currentUserService.requirePrincipal();
        return new UserSummaryDto(
                p.userId(),
                p.username(),
                p.displayName(),
                p.roles().stream().sorted().toList(),
                true
        );
    }
}

