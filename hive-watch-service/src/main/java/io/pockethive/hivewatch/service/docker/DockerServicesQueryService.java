package io.pockethive.hivewatch.service.docker;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateRepository;
import io.pockethive.hivewatch.service.api.DockerServiceListItemDto;
import io.pockethive.hivewatch.service.api.DockerServicesPageDto;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class DockerServicesQueryService {
    private final ServerRepository serverRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ActuatorTargetScanStateRepository actuatorTargetScanStateRepository;

    public DockerServicesQueryService(
            ServerRepository serverRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ActuatorTargetScanStateRepository actuatorTargetScanStateRepository
    ) {
        this.serverRepository = serverRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.actuatorTargetScanStateRepository = actuatorTargetScanStateRepository;
    }

    @Transactional(readOnly = true)
    public DockerServicesPageDto listServices(UUID environmentId, UUID serverId, String q, int page, int size) {
        if (page < 0) throw new ResponseStatusException(BAD_REQUEST, "page must be >= 0");
        if (size < 1 || size > 200) throw new ResponseStatusException(BAD_REQUEST, "size must be 1..200");

        if (!serverRepository.existsByIdAndEnvironmentId(serverId, environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Server not found");
        }

        String query = q == null ? "" : q.trim();
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "profile"));
        Page<ActuatorTargetEntity> targets = query.isEmpty()
                ? actuatorTargetRepository.findByServerId(serverId, pr)
                : actuatorTargetRepository.findByServerIdAndProfileContainingIgnoreCase(serverId, query, pr);

        List<UUID> ids = targets.getContent().stream().map(ActuatorTargetEntity::getId).toList();
        Map<UUID, ActuatorTargetScanStateEntity> stateById = actuatorTargetScanStateRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(ActuatorTargetScanStateEntity::getTargetId, Function.identity()));

        List<DockerServiceListItemDto> items = targets.getContent().stream().map(t -> {
            ActuatorTargetScanStateEntity st = stateById.get(t.getId());
            return new DockerServiceListItemDto(
                    t.getId(),
                    t.getProfile(),
                    st == null ? null : st.getAppName(),
                    st == null ? null : st.getBuildVersion(),
                    st == null ? null : st.getOutcomeKind(),
                    st == null ? null : st.getErrorKind(),
                    st == null ? null : st.getErrorMessage(),
                    st == null ? null : st.getHealthStatus(),
                    st == null ? null : st.getScannedAt()
            );
        }).toList();

        return new DockerServicesPageDto(page, size, targets.getTotalElements(), items);
    }
}

