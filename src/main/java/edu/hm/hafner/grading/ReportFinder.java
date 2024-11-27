package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Base class that finds files in the workspace.
 *
 * @author Ullrich Hafner
 */
class ReportFinder {
    /**
     * Finds reports for the specified tool.
     *
     * @param log
     *         logger
     * @param tool
     *         the tool to find the reports for
     *
     * @return the paths
     */
    List<Path> find(final FilteredLog log, final ToolConfiguration tool) {
        var displayName = tool.getDisplayName();
        var pattern = tool.getPattern();

        return find(log, displayName, pattern);
    }

    List<Path> find(final FilteredLog log, final String displayName, final String pattern) {
        log.logInfo("Searching for %s results matching file name pattern %s", displayName, pattern);
        List<Path> files = findGlob("glob:" + pattern, ".", log);

        if (files.isEmpty()) {
            log.logError("No matching report files found when using pattern '%s'! "
                    + "Configuration error for '%s'?", pattern, displayName);
        }

        Collections.sort(files);
        return files;
    }

    @VisibleForTesting
    List<Path> findGlob(final String pattern, final String directory, final FilteredLog log) {
        try {
            var visitor = new PathMatcherFileVisitor(pattern);
            Files.walkFileTree(Path.of(directory), visitor);
            return visitor.getMatches();
        }
        catch (IOException exception) {
            log.logException(exception, "Cannot find files with pattern '%s' in '%s'", pattern, directory);

            return new ArrayList<>();
        }
    }

    private static class PathMatcherFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher pathMatcher;
        private final List<Path> matches = new ArrayList<>();

        PathMatcherFileVisitor(final String syntaxAndPattern) {
            super();

            try {
                pathMatcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
            }
            catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Pattern not valid for FileSystem.getPathMatcher: " + syntaxAndPattern, exception);
            }
        }

        List<Path> getMatches() {
            return matches;
        }

        @NonNull
        @Override
        public FileVisitResult visitFile(final Path path, @NonNull final BasicFileAttributes attrs) {
            if (pathMatcher.matches(path)) {
                matches.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        @NonNull
        @Override
        public FileVisitResult visitFileFailed(final Path file, @NonNull final IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
