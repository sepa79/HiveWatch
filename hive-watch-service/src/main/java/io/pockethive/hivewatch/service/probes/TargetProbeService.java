package io.pockethive.hivewatch.service.probes;

import io.pockethive.hivewatch.service.actuator.ActuatorClient;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetValidation;
import io.pockethive.hivewatch.service.api.ActuatorHttpProbeObservedDto;
import io.pockethive.hivewatch.service.api.TargetAdapterTypeDto;
import io.pockethive.hivewatch.service.api.TargetProbeCandidateDto;
import io.pockethive.hivewatch.service.api.TargetProbeRequestDto;
import io.pockethive.hivewatch.service.api.TargetProbeResultDto;
import io.pockethive.hivewatch.service.api.TomcatManagerHtmlProbeObservedDto;
import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import io.pockethive.hivewatch.service.targets.TargetConnectionValidation;
import io.pockethive.hivewatch.service.tomcat.TomcatManagerHtmlClient;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetValidation;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class TargetProbeService {
    private final ActuatorClient actuatorClient;
    private final TomcatManagerHtmlClient tomcatManagerHtmlClient = new TomcatManagerHtmlClient();

    public TargetProbeService(ActuatorClient actuatorClient) {
        this.actuatorClient = actuatorClient;
    }

    public TargetProbeResultDto probe(TargetProbeRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.adapterType() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "adapterType is required");
        }

        return switch (request.adapterType()) {
            case TOMCAT_MANAGER_HTML -> probeTomcatManagerHtml(request);
            case ACTUATOR_HTTP -> probeActuatorHttp(request);
        };
    }

    private TargetProbeResultDto probeTomcatManagerHtml(TargetProbeRequestDto request) {
        try {
            TomcatTargetValidation.parseBaseUrl(request.baseUrl());
            TargetConnectionValidation.validatePort(request.port());
            TomcatTargetValidation.sanitizeUsername(request.username());
            TomcatTargetValidation.requirePassword(request.password());
            TargetConnectionValidation.validateTimeouts(request.connectTimeoutMs(), request.requestTimeoutMs());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }

        TomcatManagerHtmlClient.TomcatManagerFetchResult result = tomcatManagerHtmlClient.fetchSnapshot(
                new TomcatManagerHtmlClient.TomcatManagerHtmlEndpoint(
                        request.baseUrl().trim(),
                        request.port(),
                        request.username().trim(),
                        request.password(),
                        request.connectTimeoutMs(),
                        request.requestTimeoutMs()
                )
        );

        if (!result.ok()) {
            return errorResult(request, result.errorKind(), result.errorMessage());
        }

        return new TargetProbeResultDto(
                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                TomcatScanOutcomeKind.SUCCESS,
                null,
                null,
                new TomcatManagerHtmlProbeObservedDto(
                        TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                        result.tomcatVersion(),
                        result.javaVersion(),
                        result.os(),
                        result.webapps()
                ),
                candidate(request)
        );
    }

    private TargetProbeResultDto probeActuatorHttp(TargetProbeRequestDto request) {
        try {
            ActuatorTargetValidation.parseBaseUrl(request.baseUrl());
            TargetConnectionValidation.validatePort(request.port());
            ActuatorTargetValidation.sanitizeProfile(request.profile());
            TargetConnectionValidation.validateTimeouts(request.connectTimeoutMs(), request.requestTimeoutMs());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }

        ActuatorClient.ActuatorFetchResult result = actuatorClient.fetch(new ActuatorClient.ActuatorEndpoint(
                request.baseUrl().trim(),
                request.port(),
                request.profile().trim(),
                request.connectTimeoutMs(),
                request.requestTimeoutMs()
        ));

        if (!result.ok()) {
            return errorResult(request, result.errorKind(), result.errorMessage());
        }

        return new TargetProbeResultDto(
                TargetAdapterTypeDto.ACTUATOR_HTTP,
                TomcatScanOutcomeKind.SUCCESS,
                null,
                null,
                new ActuatorHttpProbeObservedDto(
                        TargetAdapterTypeDto.ACTUATOR_HTTP,
                        result.healthStatus(),
                        result.appName(),
                        result.buildVersion(),
                        result.cpuUsage(),
                        result.memoryUsedBytes()
                ),
                candidate(request)
        );
    }

    private static TargetProbeResultDto errorResult(
            TargetProbeRequestDto request,
            TomcatScanErrorKind errorKind,
            String errorMessage
    ) {
        return new TargetProbeResultDto(
                request.adapterType(),
                TomcatScanOutcomeKind.ERROR,
                errorKind,
                errorMessage,
                null,
                candidate(request)
        );
    }

    private static TargetProbeCandidateDto candidate(TargetProbeRequestDto request) {
        return new TargetProbeCandidateDto(request.baseUrl().trim(), request.port());
    }
}
