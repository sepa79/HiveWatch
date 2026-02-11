package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatWebappDto;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

final class TomcatManagerHtmlParser {
    private TomcatManagerHtmlParser() {
    }

    static TomcatManagerSnapshot parseSnapshot(String html) {
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

        Set<TomcatWebappDto> webapps = new LinkedHashSet<>();
        for (Element a : applicationsTable.select("td.row-left[rowspan] > small > a[href]")) {
            String path = normalizeText(a.text());
            if (path.isBlank()) {
                continue;
            }

            Element td = a.closest("td");
            String displayName = null;
            String version = null;
            if (td != null) {
                Element tr = td.closest("tr");
                if (tr != null) {
                    Elements tds = tr.select("> td");
                    int idx = tds.indexOf(td);
                    if (idx >= 0 && idx + 1 < tds.size()) {
                        String versionText = normalizeText(tds.get(idx + 1).text());
                        if (!versionText.isBlank()) {
                            String lower = versionText.toLowerCase();
                            if (!lower.contains("none specified")) {
                                version = versionText;
                            }
                        }
                    }
                    if (idx >= 0 && idx + 2 < tds.size()) {
                        displayName = normalizeText(tds.get(idx + 2).text());
                        displayName = firstToken(displayName);
                    }
                }
            }

            String name = defaultNameFromPath(path);
            if (displayName != null && !displayName.isBlank() && !displayName.equals(version)) {
                name = displayName;
            }

            if (version == null) {
                int versionSep = name.indexOf("##");
                if (versionSep >= 0) {
                    version = name.substring(versionSep + 2);
                    name = name.substring(0, versionSep);
                }
            }

            webapps.add(new TomcatWebappDto(path, name, version));
        }

        if (webapps.isEmpty()) {
            throw new IllegalArgumentException("Tomcat manager HTML: Applications table contained no paths");
        }

        Map<String, String> serverInfo = parseServerInfo(doc);
        String tomcatVersion = serverInfo.get("Tomcat Version");
        String javaVersion = serverInfo.get("JVM Version");
        String os = joinNonBlank(
                serverInfo.get("OS Name"),
                serverInfo.get("OS Version"),
                serverInfo.get("OS Architecture")
        );

        return new TomcatManagerSnapshot(webapps.stream().sorted((a, b) -> a.path().compareToIgnoreCase(b.path())).toList(), tomcatVersion, javaVersion, os);
    }

    private static String normalizeText(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace("\u00a0", "");
    }

    private static String firstToken(String s) {
        String trimmed = normalizeText(s);
        if (trimmed.isBlank()) {
            return trimmed;
        }
        int ws = trimmed.indexOf(' ');
        if (ws < 0) {
            return trimmed;
        }
        return trimmed.substring(0, ws);
    }

    private static String defaultNameFromPath(String path) {
        if ("/".equals(path)) {
            return "ROOT";
        }
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }

    private static Map<String, String> parseServerInfo(Document doc) {
        Element serverInfoTitle = doc.selectFirst("td.title:contains(Server Information)");
        if (serverInfoTitle == null) {
            return Map.of();
        }
        Element table = serverInfoTitle.closest("table");
        if (table == null) {
            return Map.of();
        }

        Element headerRow = table.selectFirst("tr:has(td.header-center), tr:has(td.header-left)");
        if (headerRow == null) {
            return Map.of();
        }
        Element valueRow = headerRow.nextElementSibling();
        if (valueRow == null) {
            return Map.of();
        }

        Elements headerCells = headerRow.select("td");
        Elements valueCells = valueRow.select("td");
        int n = Math.min(headerCells.size(), valueCells.size());
        if (n == 0) {
            return Map.of();
        }

        java.util.LinkedHashMap<String, String> info = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String k = normalizeText(headerCells.get(i).text());
            String v = normalizeText(valueCells.get(i).text());
            if (k.isBlank() || v.isBlank()) {
                continue;
            }
            info.put(k, v);
        }
        return Map.copyOf(info);
    }

    private static String joinNonBlank(String... parts) {
        if (parts == null || parts.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (t.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t);
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    record TomcatManagerSnapshot(
            List<TomcatWebappDto> webapps,
            String tomcatVersion,
            String javaVersion,
            String os
    ) {
    }
}
