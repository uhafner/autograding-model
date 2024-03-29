package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * Base class that finds files in the workspace.
 *
 * @author Ullrich Hafner
 */
class ReportFinder {
    /**
     * Finds reports for the specified tool.
     *
     * @param tool
     *         the tool to find the reports for
     * @param log
     *         logger
     *
     * @return the paths
     */
    public List<Path> find(final ToolConfiguration tool, final FilteredLog log) {
        log.logInfo("Searching for %s results matching file name pattern %s",
                tool.getDisplayName(), tool.getPattern());
        List<Path> files = find("glob:" + tool.getPattern(), ".", log);

        if (files.isEmpty()) {
            log.logError("No matching report files found when using pattern '%s'! "
                    + "Configuration error for '%s'?", tool.getPattern(), tool.getDisplayName());
        }

        Collections.sort(files);
        return files;
    }

    @VisibleForTesting
    List<Path> find(final String pattern, final String directory, final FilteredLog log) {
        try {
            var visitor = new PathMatcherFileVisitor(pattern);
            Files.walkFileTree(Paths.get(directory), visitor);
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

        @Override
        public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
            if (pathMatcher.matches(path)) {
                matches.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
