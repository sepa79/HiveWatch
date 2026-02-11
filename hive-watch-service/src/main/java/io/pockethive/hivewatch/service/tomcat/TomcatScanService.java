package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatTargetDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TomcatScanService {
    private final TomcatTargetRepository tomcatTargetRepository;
    private final TomcatTargetScanStateRepository tomcatTargetScanStateRepository;
    private final TomcatManagerHtmlClient tomcatManagerHtmlClient = new TomcatManagerHtmlClient();

    public TomcatScanService(
            TomcatTargetRepository tomcatTargetRepository,
            TomcatTargetScanStateRepository tomcatTargetScanStateRepository
    ) {
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.tomcatTargetScanStateRepository = tomcatTargetScanStateRepository;
    }

    @Transactional
    public List<TomcatTargetDto> scanEnvironment(UUID environmentId) {
        List<TomcatTargetEntity> targets = tomcatTargetRepository.findByEnvironmentId(environmentId);
        for (TomcatTargetEntity target : targets) {
            scanInternal(target);
        }
        Map<UUID, TomcatTargetScanStateEntity> states = tomcatTargetScanStateRepository
                .findAllById(targets.stream().map(TomcatTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(TomcatTargetScanStateEntity::getTargetId, Function.identity()));
        return targets.stream().map(t -> TomcatTargetService.toDto(t, states.get(t.getId()))).toList();
    }

    @Transactional
    public TomcatTargetDto scanTarget(UUID targetId) {
        TomcatTargetEntity target = tomcatTargetRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tomcat target not found"));

        scanInternal(target);

        TomcatTargetScanStateEntity state = tomcatTargetScanStateRepository.findById(targetId).orElse(null);
        return TomcatTargetService.toDto(target, state);
    }

    private void scanInternal(TomcatTargetEntity target) {
        Instant now = Instant.now();
        TomcatManagerHtmlClient.TomcatManagerFetchResult result = tomcatManagerHtmlClient.fetchWebapps(target);
        TomcatTargetScanStateEntity state = result.ok()
                ? TomcatTargetService.successState(target.getId(), now, result.webapps())
                : TomcatTargetService.errorState(target.getId(), now, result.errorKind(), result.errorMessage());
        tomcatTargetScanStateRepository.save(state);
    }
}

