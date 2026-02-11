package io.pockethive.hivewatch.service.tomcat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TomcatManagerHtmlParserTest {
    @Test
    void parsesApplicationsTablePaths() throws IOException {
        String html = new String(
                TomcatManagerHtmlParserTest.class.getResourceAsStream("/fixtures/tomcat-manager-sample.html").readAllBytes(),
                StandardCharsets.UTF_8
        );

        List<String> webapps = TomcatManagerHtmlParser.parseWebapps(html);
        assertEquals(List.of("/", "/PaymentApp1", "/PaymentApp2", "/manager"), webapps);
    }
}

