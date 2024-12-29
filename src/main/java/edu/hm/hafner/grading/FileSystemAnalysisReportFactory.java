package edu.hm.hafner.grading;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.AggregatedScore.AnalysisReportFactory;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;

/**
 * Reads analysis reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public final class FileSystemAnalysisReportFactory implements AnalysisReportFactory {
    private static final ReportFinder REPORT_FINDER = new ReportFinder();
    private static final PathUtil PATH_UTIL = new PathUtil();

    @Override
    public Report create(final ToolConfiguration tool, final FilteredLog log) {
        var parser = new ParserRegistry().get(tool.getId());

        var displayName = StringUtils.defaultIfBlank(tool.getName(), parser.getName());
        var total = new Report(tool.getId(), displayName);
        total.setIcon(tool.getIcon());

        var analysisParser = parser.createParser();
        for (Path file : REPORT_FINDER.find(log, displayName, tool.getPattern())) {
            var report = analysisParser.parse(new FileReaderFactory(file));
            total.addAll(report);
            log.logInfo("- %s: %s", PATH_UTIL.getRelativePath(file), report.getSummary());
        }

        log.logInfo("-> %s", total.toString());
        return total;
    }
}
