package io.pockethive.hivewatch.service.environments.servers;

import io.pockethive.hivewatch.service.api.ServerCreateRequestDto;
import io.pockethive.hivewatch.service.api.ServerDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {
    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/servers")
    public List<ServerDto> list(@PathVariable("environmentId") UUID environmentId) {
        return serverService.list(environmentId);
    }

    @PostMapping("/api/v1/environments/{environmentId}/servers")
    public ServerDto create(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody ServerCreateRequestDto request
    ) {
        return serverService.create(environmentId, request);
    }
}

