package io.pockethive.hivewatch.service.users;

import io.pockethive.hivewatch.service.api.UserSummaryDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/api/v1/admin/users")
    public List<UserSummaryDto> list() {
        return List.of(
            new UserSummaryDto(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "local-admin",
                "Local Admin",
                List.of("ADMIN"),
                true
            )
        );
    }
}

