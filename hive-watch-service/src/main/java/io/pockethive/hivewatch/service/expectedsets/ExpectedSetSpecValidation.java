package io.pockethive.hivewatch.service.expectedsets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ExpectedSetSpecValidation {
    private static final Set<String> BUILT_IN_TOMCAT_WEBAPPS = Set.of("/", "/manager", "/host-manager", "/docs", "/examples");

    private ExpectedSetSpecValidation() {
    }

    public static List<ExpectedSetItemValidationIssue> validateTomcatWebappPaths(List<String> items) {
        List<ExpectedSetItemValidationIssue> issues = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            String path = normalized(items.get(i));
            if (path == null) {
                issues.add(new ExpectedSetItemValidationIssue(i, "items cannot contain empty values"));
                continue;
            }
            if (!path.startsWith("/")) {
                issues.add(new ExpectedSetItemValidationIssue(i, "Webapp path must start with '/': " + path));
            }
            if (BUILT_IN_TOMCAT_WEBAPPS.contains(path)) {
                issues.add(new ExpectedSetItemValidationIssue(i, "Built-in Tomcat webapp is not allowed in expected list: " + path));
            }
            if (!seen.add(path)) {
                issues.add(new ExpectedSetItemValidationIssue(i, "Duplicate item: " + path));
            }
        }
        return List.copyOf(issues);
    }

    public static List<ExpectedSetItemValidationIssue> validateDockerProfiles(List<String> items) {
        List<ExpectedSetItemValidationIssue> issues = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            String profile = normalized(items.get(i));
            if (profile == null) {
                issues.add(new ExpectedSetItemValidationIssue(i, "items cannot contain empty values"));
                continue;
            }
            if (!seen.add(profile)) {
                issues.add(new ExpectedSetItemValidationIssue(i, "Duplicate item: " + profile));
            }
        }
        return List.copyOf(issues);
    }

    private static String normalized(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isBlank() ? null : value;
    }
}
