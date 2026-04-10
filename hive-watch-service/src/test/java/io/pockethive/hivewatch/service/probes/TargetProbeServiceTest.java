package io.pockethive.hivewatch.service.probes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.pockethive.hivewatch.service.actuator.ActuatorClient;
import io.pockethive.hivewatch.service.api.ActuatorHttpProbeObservedDto;
import io.pockethive.hivewatch.service.api.TargetAdapterTypeDto;
import io.pockethive.hivewatch.service.api.TargetProbeObservedDto;
import io.pockethive.hivewatch.service.api.TargetProbeRequestDto;
import io.pockethive.hivewatch.service.api.TargetProbeResultDto;
import io.pockethive.hivewatch.service.api.TomcatManagerHtmlProbeObservedDto;
import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TargetProbeServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void probesTomcatManagerHtmlCandidate() throws Exception {
        String html = new String(
                getClass().getResourceAsStream("/fixtures/tomcat-manager-sample.html").readAllBytes(),
                StandardCharsets.UTF_8
        );
        startServer();
        server.createContext("/manager/html", exchange -> respond(exchange, 200, "text/html", html));

        TargetProbeResultDto result = service().probe(new TargetProbeRequestDto(
                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                baseUrl(),
                port(),
                "hc-manager",
                "hc-manager-pass",
                null,
                1000,
                3000
        ));

        assertThat(result.adapterType()).isEqualTo(TargetAdapterTypeDto.TOMCAT_MANAGER_HTML);
        assertThat(result.outcomeKind()).isEqualTo(TomcatScanOutcomeKind.SUCCESS);
        assertThat(result.errorKind()).isNull();
        assertThat(result.candidate().baseUrl()).isEqualTo(baseUrl());
        assertThat(result.candidate().port()).isEqualTo(port());

        TargetProbeObservedDto observed = result.observed();
        assertThat(observed).isInstanceOf(TomcatManagerHtmlProbeObservedDto.class);
        TomcatManagerHtmlProbeObservedDto tomcatObserved = (TomcatManagerHtmlProbeObservedDto) observed;
        assertThat(tomcatObserved.adapterType()).isEqualTo(TargetAdapterTypeDto.TOMCAT_MANAGER_HTML);
        assertThat(tomcatObserved.webapps()).extracting("path").contains("/PaymentApp1", "/PaymentApp2");
    }

    @Test
    void probesActuatorHttpCandidate() throws Exception {
        startServer();
        server.createContext("/payments/actuator/health", exchange -> respond(exchange, 200, "application/json", "{\"status\":\"UP\"}"));
        server.createContext("/payments/actuator/info", exchange -> respond(exchange, 200, "application/json", "{\"app\":{\"name\":\"payments-service\",\"build\":{\"version\":\"2.0.0\"}}}"));
        server.createContext("/payments/actuator/metrics/system.cpu.usage", exchange -> respond(exchange, 200, "application/json", "{\"measurements\":[{\"value\":0.25}]}"));
        server.createContext("/payments/actuator/metrics/jvm.memory.used", exchange -> respond(exchange, 200, "application/json", "{\"measurements\":[{\"value\":123456}]}"));

        TargetProbeResultDto result = service().probe(new TargetProbeRequestDto(
                TargetAdapterTypeDto.ACTUATOR_HTTP,
                baseUrl(),
                port(),
                null,
                null,
                "payments",
                1000,
                3000
        ));

        assertThat(result.adapterType()).isEqualTo(TargetAdapterTypeDto.ACTUATOR_HTTP);
        assertThat(result.outcomeKind()).isEqualTo(TomcatScanOutcomeKind.SUCCESS);
        assertThat(result.errorKind()).isNull();

        TargetProbeObservedDto observed = result.observed();
        assertThat(observed).isInstanceOf(ActuatorHttpProbeObservedDto.class);
        ActuatorHttpProbeObservedDto actuatorObserved = (ActuatorHttpProbeObservedDto) observed;
        assertThat(actuatorObserved.adapterType()).isEqualTo(TargetAdapterTypeDto.ACTUATOR_HTTP);
        assertThat(actuatorObserved.healthStatus()).isEqualTo("UP");
        assertThat(actuatorObserved.appName()).isEqualTo("payments-service");
        assertThat(actuatorObserved.buildVersion()).isEqualTo("2.0.0");
        assertThat(actuatorObserved.cpuUsage()).isEqualTo(0.25);
        assertThat(actuatorObserved.memoryUsedBytes()).isEqualTo(123456L);
    }

    @Test
    void returnsClassifiedErrorWithoutMutatingConfig() throws Exception {
        startServer();
        server.createContext("/manager/html", exchange -> respond(exchange, 401, "text/plain", "no"));

        TargetProbeResultDto result = service().probe(new TargetProbeRequestDto(
                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                baseUrl(),
                port(),
                "hc-manager",
                "wrong",
                null,
                1000,
                3000
        ));

        assertThat(result.outcomeKind()).isEqualTo(TomcatScanOutcomeKind.ERROR);
        assertThat(result.errorKind()).isEqualTo(TomcatScanErrorKind.AUTH);
        assertThat(result.observed()).isNull();
    }

    @Test
    void rejectsBaseUrlWithPortBeforeProbe() {
        assertThatThrownBy(() -> service().probe(new TargetProbeRequestDto(
                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                "http://localhost:8080",
                8081,
                "hc-manager",
                "hc-manager-pass",
                null,
                1000,
                3000
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("baseUrl must not include port");
    }

    private TargetProbeService service() {
        return new TargetProbeService(new ActuatorClient(new ObjectMapper()));
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1";
    }

    private int port() {
        return server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
