package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.coverage.registry.ParserRegistry;
import edu.hm.hafner.grading.AggregatedScore.CoverageReportFactory;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

/**
 * Reads coverage reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public final class FileSystemCoverageReportFactory implements CoverageReportFactory {
    private static final ReportFinder REPORT_FINDER = new ReportFinder();
    private static final PathUtil PATH_UTIL = new PathUtil();

    @Override
    public Node create(final ToolConfiguration tool, final FilteredLog log) {
        var parser = new ParserRegistry().get(StringUtils.upperCase(tool.getId()), ProcessingMode.IGNORE_ERRORS);

        var nodes = new ArrayList<Node>();
        for (Path file : REPORT_FINDER.find(log, tool)) {
            var node = parser.parse(new FileReaderFactory(file).create(), file.toString(), log);
            log.logInfo("- %s: %s", PATH_UTIL.getRelativePath(file), extractMetric(tool, node));
            nodes.add(node);
        }

        if (nodes.isEmpty()) {
            return createEmptyContainer(tool);
        }
        else {
            var aggregation = Node.merge(nodes);
            log.logInfo("-> %s Total: %s", tool.getDisplayName(), extractMetric(tool, aggregation));
            if (tool.getName().isBlank()) {
                return aggregation;
            }
            // Wrap the node into a container with the specified tool name
            var containerNode = createEmptyContainer(tool);
            containerNode.addChild(aggregation);
            return containerNode;
        }
    }

    private ContainerNode createEmptyContainer(final ToolConfiguration tool) {
        return new ContainerNode(tool.getName());
    }

    private String extractMetric(final ToolConfiguration tool, final Node node) {
        return node.getValue(Metric.valueOf(StringUtils.upperCase(tool.getMetric())))
                .map(Value::toString)
                .orElse("<none>");
    }
}
