package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;

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

import static edu.hm.hafner.grading.ScoreBuilder.*;

/**
 * Base class that finds files in the workspace.
 *
 * @author Ullrich Hafner
 */
class ReportFinder {
    List<Path> find(final FilteredLog log, final String displayName, final String pattern,
            final String directory, final String excludedDirectory) {
        log.logInfo("Searching for %s results in folder '%s' matching file name pattern '%s'%s",
                displayName, directory, pattern, exclude(excludedDirectory));
        List<Path> files = findGlob("glob:" + pattern, directory, excludedDirectory, log);

        if (files.isEmpty()) {
            log.logInfo("No matching report files found in folder '%s' when using pattern '%s'! "
                    + "Configuration error for '%s'?", directory, pattern, displayName);
        }

        Collections.sort(files);
        return files;
    }

    private String exclude(final String excludedDirectory) {
        if (NO_DELTA_REPORTS.equals(excludedDirectory)) {
            return StringUtils.EMPTY;
        }
        return String.format(" (excluding '%s')", excludedDirectory);
    }

    @VisibleForTesting
    List<Path> findGlob(final String pattern, final String directory, final String excludedDirectory,
            final FilteredLog log) {
        try {
            var visitor = new PathMatcherFileVisitor(pattern, excludedDirectory);
            Files.walkFileTree(Path.of(directory), visitor);
            return visitor.getMatches();
        }
        catch (IOException exception) {
            log.logException(exception, "Cannot find files with pattern '%s' in '%s'", pattern, directory);

            return new ArrayList<>();
        }
    }

    private static class PathMatcherFileVisitor extends SimpleFileVisitor<Path> {
        private static final String DEFAULT_WORK_DIRECTORY = ".";
        private final PathMatcher pathMatcher;
        private final List<Path> matches = new ArrayList<>();
        private final String excludeDirectory;

        PathMatcherFileVisitor(final String syntaxAndPattern, final String excludeDirectory) {
            super();

            this.excludeDirectory = excludeDirectory;
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
        public FileVisitResult visitFile(@NonNull final Path path, @NonNull final BasicFileAttributes attrs) {
            if (pathMatcher.matches(path) && isNotExcluded(path)) {
                matches.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        private boolean isNotExcluded(final Path path) {
            if (excludeDirectory.isBlank() || excludeDirectory.equals(NO_DELTA_REPORTS)) {
                return true;
            }

            var excluded = Path.of(excludeDirectory).toAbsolutePath().normalize();
            return !path.toAbsolutePath().normalize().startsWith(excluded);
        }

        @NonNull
        @Override
        public FileVisitResult visitFileFailed(@NonNull final Path file, @NonNull final IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
