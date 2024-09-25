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

        var displayName = getDisplayName(tool, parser.getName());
        var total = new Report(tool.getId(), displayName);

        var analysisParser = parser.createParser();
        for (Path file : REPORT_FINDER.find(log, displayName, tool.getPattern())) {
            var report = analysisParser.parseFile(new FileReaderFactory(file));
            report.setOrigin(tool.getId(), displayName);
            log.logInfo("- %s: %d warnings", PATH_UTIL.getRelativePath(file), report.size());
            total.addAll(report);
        }

        log.logInfo("-> %s Total: %d warnings", displayName, total.size());
        return total;
    }

    private String getDisplayName(final ToolConfiguration tool, final String defaultName) {
        return StringUtils.defaultIfBlank(tool.getName(), defaultName);
    }
}
