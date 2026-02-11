package io.pockethive.hivewatch.service.scans;

import io.pockethive.hivewatch.service.actuator.ActuatorScanService;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatScanService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScanSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(ScanSchedulerService.class);

    private final ScanSchedulerProperties properties;
    private final EnvironmentRepository environmentRepository;
    private final TomcatScanService tomcatScanService;
    private final ActuatorScanService actuatorScanService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ScanSchedulerService(
            ScanSchedulerProperties properties,
            EnvironmentRepository environmentRepository,
            TomcatScanService tomcatScanService,
            ActuatorScanService actuatorScanService
    ) {
        this.properties = properties;
        this.environmentRepository = environmentRepository;
        this.tomcatScanService = tomcatScanService;
        this.actuatorScanService = actuatorScanService;
    }

    @Scheduled(
            fixedDelayString = "${hivewatch.scans.scheduler.fixed-delay-ms}",
            initialDelayString = "${hivewatch.scans.scheduler.initial-delay-ms}"
    )
    public void runOnce() {
        if (!properties.enabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            environmentRepository.findAll().forEach(env -> {
                try {
                    tomcatScanService.scanEnvironment(env.getId());
                } catch (RuntimeException e) {
                    log.warn("Scheduled Tomcat scan failed for envId={}: {}", env.getId(), e.getMessage());
                }
                try {
                    actuatorScanService.scanEnvironment(env.getId());
                } catch (RuntimeException e) {
                    log.warn("Scheduled actuator scan failed for envId={}: {}", env.getId(), e.getMessage());
                }
            });
        } finally {
            running.set(false);
        }
    }
}

