package io.pockethive.hivewatch.service.tomcat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TomcatManagerHtmlParserTest {
    @Test
    void parsesApplicationsTablePaths() throws IOException {
        String html = new String(
                TomcatManagerHtmlParserTest.class.getResourceAsStream("/fixtures/tomcat-manager-sample.html").readAllBytes(),
                StandardCharsets.UTF_8
        );

        TomcatManagerHtmlParser.TomcatManagerSnapshot snapshot = TomcatManagerHtmlParser.parseSnapshot(html);
        assertEquals(
                Set.of("/", "/PaymentApp1", "/PaymentApp2", "/manager"),
                snapshot.webapps().stream().map(w -> w.path()).collect(java.util.stream.Collectors.toSet())
        );
    }
}
