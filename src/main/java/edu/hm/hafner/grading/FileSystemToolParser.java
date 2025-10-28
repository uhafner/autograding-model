package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.IssuesInModifiedCodeMarker;
import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Reads analysis or coverage reports of a specific type from the file system into a corresponding Java model.
 *
 * @author Ullrich Hafner
 */
public final class FileSystemToolParser implements ToolParser {
    private static final ReportFinder REPORT_FINDER = new ReportFinder();
    private static final PathUtil PATH_UTIL = new PathUtil();

    private final Map<String, Set<Integer>> modifiedLines;

    public FileSystemToolParser() {
        this(Map.of());
    }

    public FileSystemToolParser(final Map<String, Set<Integer>> modifiedLines) {
        this.modifiedLines = modifiedLines;
    }

    @Override
    public Report readReport(final ToolConfiguration tool, final FilteredLog log) {
        var parser = new ParserRegistry().get(tool.getId());

        var displayName = StringUtils.defaultIfBlank(tool.getName(), parser.getName());
        var total = new Report(tool.getId(), displayName);
        total.setIcon(tool.getIcon());

        var analysisParser = parser.createParser();
        for (Path file : REPORT_FINDER.find(log, displayName, tool.getPattern())) {
            var report = analysisParser.parse(new FileReaderFactory(file));
            var baseline = Baseline.fromString(tool.getBaseline());

            var marker = new IssuesInModifiedCodeMarker();
            marker.markIssuesInModifiedCode(report, modifiedLines);

            if (baseline == Baseline.PROJECT) {
                total.addAll(report);
            }
            else {
                total.addAll(report.getInModifiedCode());
            }
            log.logInfo("- %s: %s", PATH_UTIL.getRelativePath(file), report.getSummary());
        }

        log.logInfo("-> %s", total.toString());
        return total;
    }

    @Override
    public Node readNode(final ToolConfiguration tool, final FilteredLog log) {
        var parser = new edu.hm.hafner.coverage.registry.ParserRegistry().get(StringUtils.upperCase(tool.getId()),
                ProcessingMode.IGNORE_ERRORS);

        var nodes = new ArrayList<Node>();
        for (Path file : REPORT_FINDER.find(log, getDisplayName(tool), tool.getPattern())) {
            var factory = new FileReaderFactory(file);
            try (var reader = factory.create()) {
                var node = parser.parse(reader, file.toString(), log);

                for (var fileNode : node.getAllFileNodes()) {
                    var filePath = tool.getSourcePath() + "/" + fileNode.getRelativePath();
                    if (modifiedLines.containsKey(filePath)) {
                        fileNode.addModifiedLines(modifiedLines.get(filePath).stream().mapToInt(Integer::intValue).toArray());
                    }
                }

                var baseline = Baseline.fromString(tool.getBaseline());
                Node result = switch (baseline) {
                    case MODIFIED_FILES -> node.filterByModifiedFiles();
                    case MODIFIED_LINES -> node.filterByModifiedLines();
                    default -> node;
                };

                log.logInfo("- %s: %s", PATH_UTIL.getRelativePath(file), extractMetric(tool, result));
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
            log.logInfo("-> %s Total: %s", getDisplayName(tool), extractMetric(tool, aggregation));
            // Wrap the node into a container with the specified tool name
            var containerNode = createEmptyContainer(tool);
            containerNode.addChild(aggregation);
            return containerNode;
        }
    }

    private ContainerNode createEmptyContainer(final ToolConfiguration tool) {
        return new ContainerNode(getDisplayName(tool));
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
