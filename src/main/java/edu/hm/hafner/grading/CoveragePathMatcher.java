package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Matches coverage file paths against PR diff paths using enhanced algorithms.
 * Supports multiple coverage tools and multi-module projects through bidirectional suffix matching
 * and module context extraction.
 *
 * <p>This class is intentionally decoupled from line number data - it only needs file paths
 * for matching purposes. This enables reuse in other contexts like diff annotations.</p>
 *
 * @author Apoorva Mahabaleshwara
 */
class CoveragePathMatcher {
    private final Set<String> modifiedFiles;

    /**
     * Creates a new path matcher with the set of modified file paths from a PR diff.
     *
     * @param modifiedFiles set of repository-relative file paths that were modified in the PR
     */
    CoveragePathMatcher(final Set<String> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    /**
     * Finds a matching PR diff path for a given coverage file path using multiple strategies.
     *
     * <p>Strategies applied in order:</p>
     * <ol>
     *   <li>Exact match (for simple cases)</li>
     *   <li>Bidirectional suffix matching (handles all coverage tools)</li>
     *   <li>Module context verification (for multi-module disambiguation)</li>
     * </ol>
     *
     * @param coveragePath the relative path from the coverage report (e.g., "com/intuit/MyClass.java")
     * @param sourcePath the configured source path (may be empty, used as hint)
     * @param reportFile the path to the coverage report file (used for module root extraction)
     * @return an Optional containing the matching diff path key, or empty if no match found
     */
    Optional<String> findMatch(final String coveragePath, final String sourcePath, final Path reportFile) {
        String normalizedCoveragePath = normalizePath(coveragePath);

        // Strategy 1: Exact match (fast path for simple cases)
        String exactPath = createExactPath(sourcePath, normalizedCoveragePath);
        if (modifiedFiles.contains(exactPath)) {
            return Optional.of(exactPath);
        }

        // Strategy 2: Bidirectional suffix matching with optional module context
        for (String diffPath : modifiedFiles) {
            String normalizedDiffPath = normalizePath(diffPath);

            // Check bidirectional suffix match
            if (!isBidirectionalSuffixMatch(normalizedCoveragePath, normalizedDiffPath)) {
                continue;
            }

            // Extract module context only when we have a suffix match (lazy evaluation)
            Optional<String> moduleRoot = extractModuleRoot(reportFile);

            // If we have module context, verify it matches to disambiguate
            if (moduleRoot.isPresent()) {
                String normalizedModuleRoot = normalizePath(moduleRoot.get());
                if (isModuleMatch(normalizedDiffPath, normalizedModuleRoot)) {
                    return Optional.of(diffPath); // Module-verified match!
                }
            }
            else {
                // No module context (simple project) - suffix match is sufficient
                return Optional.of(diffPath);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates the exact path by combining source path and coverage path.
     *
     * @param sourcePath the configured source path (may be empty)
     * @param normalizedCoveragePath the normalized coverage path (may be empty)
     * @return the combined exact path
     */
    private String createExactPath(final String sourcePath, final String normalizedCoveragePath) {
        if (sourcePath.isEmpty()) {
            return normalizedCoveragePath;
        }
        else if (normalizedCoveragePath.isEmpty()) {
            return sourcePath;
        }
        else {
            return sourcePath + "/" + normalizedCoveragePath;
        }
    }

    /**
     * Checks if a diff path matches the expected module context.
     *
     * @param diffPath the normalized diff path from the PR
     * @param moduleRoot the normalized module root extracted from the report
     * @return true if the diff path contains or starts with the module root
     */
    private boolean isModuleMatch(final String diffPath, final String moduleRoot) {
        return diffPath.contains(moduleRoot + "/")
                || diffPath.startsWith(moduleRoot + "/")
                || diffPath.equals(moduleRoot);
    }

    /**
     * Checks if two paths match bidirectionally (either can be a suffix of the other).
     * Handles both directions to support different coverage tool behaviors:
     * <ul>
     *   <li>JaCoCo/Cobertura: coverage path is suffix of diff path</li>
     *   <li>Clover/OpenCover/Go: diff path is suffix of coverage path (absolute/module paths)</li>
     * </ul>
     *
     * @param path1 first path to compare
     * @param path2 second path to compare
     * @return true if one path is a suffix of the other
     */
    private boolean isBidirectionalSuffixMatch(final String path1, final String path2) {
        if (path1.equals(path2)) {
            return true;
        }

        return isPathSuffix(path1, path2) || isPathSuffix(path2, path1);
    }

    /**
     * Checks if path2 is a suffix of path1.
     *
     * @param path1 the full path
     * @param path2 the potential suffix
     * @return true if path1 ends with path2
     */
    private boolean isPathSuffix(final String path1, final String path2) {
        return path1.endsWith("/" + path2) || path1.endsWith(path2);
    }

    /**
     * Extracts the module root directory from a coverage report file path.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"module-a/target/site/jacoco/jacoco.xml" → "module-a"</li>
     *   <li>"app/build/reports/jacoco/test/jacocoTestReport.xml" → "app"</li>
     *   <li>"target/jacoco.xml" → empty (root-level, single module)</li>
     *   <li>"src/Helper.Test/bin/Debug/coverage.xml" → "Helper.Test" (.NET)</li>
     * </ul>
     *
     * @param reportPath the path to the coverage report file
     * @return an Optional containing the module root path, or empty if no module structure detected
     */
    private Optional<String> extractModuleRoot(final Path reportPath) {
        String pathStr = normalizePath(reportPath.toString());

        // Maven: look for /target/
        int targetIndex = pathStr.lastIndexOf("/target/");
        if (targetIndex > 0) {
            String beforeTarget = pathStr.substring(0, targetIndex);
            // Get just the last segment (module name) if it's a nested path
            int lastSlash = beforeTarget.lastIndexOf('/');
            return Optional.of(lastSlash >= 0 ? beforeTarget.substring(lastSlash + 1) : beforeTarget);
        }

        // Gradle: look for /build/
        int buildIndex = pathStr.lastIndexOf("/build/");
        if (buildIndex > 0) {
            String beforeBuild = pathStr.substring(0, buildIndex);
            int lastSlash = beforeBuild.lastIndexOf('/');
            return Optional.of(lastSlash >= 0 ? beforeBuild.substring(lastSlash + 1) : beforeBuild);
        }

        // .NET: look for /bin/ or /obj/
        int binIndex = pathStr.lastIndexOf("/bin/");
        if (binIndex > 0) {
            String beforeBin = pathStr.substring(0, binIndex);
            int lastSlash = beforeBin.lastIndexOf('/');
            return Optional.of(lastSlash >= 0 ? beforeBin.substring(lastSlash + 1) : beforeBin);
        }

        int objIndex = pathStr.lastIndexOf("/obj/");
        if (objIndex > 0) {
            String beforeObj = pathStr.substring(0, objIndex);
            int lastSlash = beforeObj.lastIndexOf('/');
            return Optional.of(lastSlash >= 0 ? beforeObj.substring(lastSlash + 1) : beforeObj);
        }

        // No standard build directory found - assume single-module project
        return Optional.empty();
    }

    /**
     * Normalizes a file path for comparison.
     * <ul>
     *   <li>Converts backslashes to forward slashes (Windows compatibility)</li>
     *   <li>Removes leading/trailing slashes</li>
     *   <li>Handles empty strings</li>
     * </ul>
     *
     * @param path the path to normalize
     * @return the normalized path
     */
    private String normalizePath(final String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        String normalized = path.replace('\\', '/');

        // Remove leading slashes
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // Remove trailing slashes
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}
