package edu.hm.hafner.grading;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps SCM file paths (e.g., from a pull request) to coverage report file paths using longest-suffix matching.
 * This mirrors the approach used in the Jenkins coverage plugin to align repository paths with report-relative paths.
 */
final class PathMapper {
    private static final String AMBIGUOUS_PATHS_ERROR =
            "Failed to map SCM paths with coverage report paths due to ambiguous fully qualified names";
    /**
     * Creates a mapping from SCM paths to coverage report paths.
     * If no match is found for a given SCM path, the value will be an empty string.
     */
    Map<String, String> mapScmToReportPaths(final Collection<String> scmPaths, final Collection<String> reportPaths) {
        Map<String, String> mapping = new HashMap<>();
        for (String scmPath : scmPaths) {
            reportPaths.stream()
                    .filter(scmPath::endsWith)
                    .max(Comparator.comparingInt(String::length))
                    .ifPresentOrElse(match -> mapping.put(scmPath, match),
                            () -> mapping.put(scmPath, ""));
        }
        verifyUniqueMapping(mapping);
        return mapping;
    }

    private void verifyUniqueMapping(final Map<String, String> mapping) {
        List<String> notEmptyValues = mapping.values().stream()
                .filter(path -> !path.isEmpty())
                .collect(Collectors.toList());
        if (notEmptyValues.size() != new HashSet<>(notEmptyValues).size()) {
            throw new IllegalStateException(AMBIGUOUS_PATHS_ERROR);
        }
    }
}




