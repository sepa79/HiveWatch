package io.pockethive.hivewatch.service.tomcat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

final class TomcatManagerHtmlParser {
    private TomcatManagerHtmlParser() {
    }

    static List<String> parseWebapps(String html) {
        Objects.requireNonNull(html, "html");
        Document doc = Jsoup.parse(html);

        Element applicationsTitle = doc.selectFirst("td.title:contains(Applications)");
        if (applicationsTitle == null) {
            throw new IllegalArgumentException("Tomcat manager HTML: missing Applications table");
        }

        Element applicationsTable = applicationsTitle.closest("table");
        if (applicationsTable == null) {
            throw new IllegalArgumentException("Tomcat manager HTML: missing Applications table wrapper");
        }

        Set<String> paths = new LinkedHashSet<>();
        for (Element a : applicationsTable.select("td.row-left[rowspan] > small > a[href]")) {
            String text = a.text();
            if (text == null) {
                continue;
            }
            String trimmed = text.trim().replace("\u00a0", "");
            if (trimmed.isBlank()) {
                continue;
            }
            paths.add(trimmed);
        }

        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Tomcat manager HTML: Applications table contained no paths");
        }

        return paths.stream().sorted().toList();
    }
}

