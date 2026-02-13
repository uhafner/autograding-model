package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.IssuesInModifiedCodeMarker;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads analysis or coverage reports of a specific type from the file system into a corresponding Java model.
 *
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public final class FileSystemToolParser implements ToolParser {
    private static final ReportFinder REPORT_FINDER = new ReportFinder();
    private static final PathUtil PATH_UTIL = new PathUtil();

    private final Map<String, Set<Integer>> modifiedLines;

    /**
     * Creates a new parser without information about modified lines in files.
     */
    public FileSystemToolParser() {
        this(Map.of());
    }

    /**
     * Creates a new parser with information about modified lines in files.
     *
     * @param modifiedLines
     *         the map of changed file paths to their changed lines
     */
    public FileSystemToolParser(final Map<String, Set<Integer>> modifiedLines) {
        this.modifiedLines = modifiedLines;
    }

    @Override
    public Report readReport(final ToolConfiguration tool, final String directory, final FilteredLog log) {
        var parser = new ParserRegistry().get(tool.getId());

        var displayName = StringUtils.defaultIfBlank(tool.getName(), parser.getName());
        var total = new Report(tool.getId(), displayName);
        total.setIcon(tool.getIcon());

        var analysisParser = parser.createParser();
        var scope = tool.getScope();
        for (Path file : REPORT_FINDER.find(log, displayName, tool.getPattern(), directory)) {
            var report = analysisParser.parse(new FileReaderFactory(file));

            if (scope == Scope.PROJECT) {
                total.addAll(report);
            }
            else {
                var marker = new IssuesInModifiedCodeMarker();
                if (scope == Scope.MODIFIED_FILES) {
                    marker.markIssuesInModifiedFiles(report, modifiedLines.keySet());
                }
                else if (scope == Scope.MODIFIED_LINES) {
                    marker.markIssuesInModifiedCode(report, modifiedLines);
                }
                total.addAll(report.getInModifiedCode());
            }

            log.logInfo("- %s: %s [Whole Project]", PATH_UTIL.getRelativePath(file), report.getSummary());
        }

        log.logInfo("-> %s [%s]", total.toString(), scope.getDisplayName());
        return total;
    }

    @Override
    public Node readNode(final ToolConfiguration tool, final String directory, final FilteredLog log) {
        var parser = new edu.hm.hafner.coverage.registry.ParserRegistry().get(StringUtils.upperCase(tool.getId()),
                ProcessingMode.IGNORE_ERRORS);
        var scope = tool.getScope();

        var nodes = new ArrayList<Node>();
        for (Path file : REPORT_FINDER.find(log, getDisplayName(tool), tool.getPattern(), directory)) {
            var factory = new FileReaderFactory(file);
            try (var reader = factory.create()) {
                var node = parser.parse(reader, file.toString(), log);

                // Enhanced path matching with module context (only relevant for non-PROJECT scopes)
                filterNodesByModifiedFiles(node.getAllFileNodes(), tool.getSourcePath(), file, scope, log);

                log.logInfo("- %s: %s [Whole Project]", PATH_UTIL.getRelativePath(file), extractMetric(tool, node));

                var result = switch (scope) {
                    case MODIFIED_FILES -> node.filterByModifiedFiles();
                    case MODIFIED_LINES -> node.filterByModifiedLines();
                    default -> node;
                };

                if (scope != Scope.PROJECT) {
                    log.logInfo("- %s: %s [%s]", PATH_UTIL.getRelativePath(file), extractMetric(tool, result), scope.getDisplayName());
                }
                nodes.add(result);
            }
            catch (IOException exception) {
                throw new ParsingException(exception);
            }
        }

        if (nodes.isEmpty()) {
            return createEmptyContainer(tool);
        }
        else {
            var aggregation = Node.merge(nodes);
            log.logInfo("-> %s Total: %s [%s]", getDisplayName(tool), extractMetric(tool, aggregation), scope.getDisplayName());
            // Wrap the node into a container with the specified tool name
            var containerNode = createEmptyContainer(tool);
            containerNode.addChild(aggregation);
            return containerNode;
        }
    }

    /**
     * Filters file nodes by matching their paths against modified lines from PR diffs.
     * Uses enhanced bidirectional suffix matching to support multiple coverage tools and multi-module projects.
     *
     * @param files the list of file nodes from the coverage report
     * @param sourcePath the configured source path (may be empty)
     * @param reportFile the path to the coverage report file (used for module root extraction)
     * @param scope the scope of the tool configuration (determines logging behavior)
     * @param log logger for debug information
     */
    private void filterNodesByModifiedFiles(final List<FileNode> files, final String sourcePath,
                                           final Path reportFile, final Scope scope, final FilteredLog log) {
        if (modifiedLines.isEmpty()) {
            return; // No modified lines to filter
        }

        var pathMatcher = new CoveragePathMatcher(modifiedLines.keySet());
        int matchedFiles = 0;
        var unmatchedFilesList = new ArrayList<String>();

        for (var file : files) {
            String coveragePath = file.getRelativePath();
            var matchedDiffPath = pathMatcher.findMatch(coveragePath, sourcePath, reportFile);

            if (matchedDiffPath.isPresent()) {
                var lines = modifiedLines.get(matchedDiffPath.get());
                if (lines != null) {
                    file.addModifiedLines(lines.stream()
                            .mapToInt(Integer::intValue)
                            .toArray());
                    matchedFiles++;
                }
            }
            else {
                unmatchedFilesList.add(coveragePath);
            }
        }

        logMatchResults(matchedFiles, unmatchedFilesList, scope, log);
    }

    /**
     * Logs the results of the file matching process, including matched and unmatched file counts.
     * Only logs detailed information for scopes that actually use modified lines/files.
     *
     * @param matchedFiles the number of successfully matched files
     * @param unmatchedFiles the list of coverage file paths that were not matched
     * @param scope the scope of the tool configuration
     * @param log logger for output
     */
    private void logMatchResults(final int matchedFiles, final List<String> unmatchedFiles,
                                 final Scope scope, final FilteredLog log) {
        boolean requiresMatching = scope == Scope.MODIFIED_LINES || scope == Scope.MODIFIED_FILES;

        if (matchedFiles > 0) {
            log.logInfo("Successfully matched %d coverage files to PR diff files", matchedFiles);
        }
        else if (requiresMatching) {
            log.logError("No coverage files matched to PR diff files!");
        }

        // Only show unmatched files note for modified scopes (not for PROJECT scope)
        if (requiresMatching && !unmatchedFiles.isEmpty()) {
            log.logInfo("Note: %d coverage file(s) were not modified in this PR (expected):", unmatchedFiles.size());
            for (String unmatched : unmatchedFiles) {
                log.logInfo("  - Unmatched: %s", unmatched);
            }
        }
    }

    private ContainerNode createEmptyContainer(final ToolConfiguration tool) {
        return new ContainerNode("%s_%s".formatted(getDisplayName(tool), tool.getScope()));
    }

    private String getDisplayName(final ToolConfiguration tool) {
        return StringUtils.defaultIfBlank(tool.getName(), getMetric(tool).getDisplayName());
    }

    String extractMetric(final ToolConfiguration tool, final Node node) {
        return node.getValue(getMetric(tool))
                .map(Value::toString)
                .orElse("<none>");
    }

    private Metric getMetric(final ToolConfiguration tool) {
        if (StringUtils.isNotBlank(tool.getMetric())) {
            return Metric.fromName(tool.getMetric());
        }
        return Metric.TESTS;
    }
}
