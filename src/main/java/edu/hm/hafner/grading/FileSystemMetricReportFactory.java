package edu.hm.hafner.grading;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.grading.AggregatedScore.CoverageReportFactory;
import edu.hm.hafner.util.FilteredLog;

/**
 * Reads metric reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public final class FileSystemMetricReportFactory implements CoverageReportFactory {
    @Override
    public Node create(final ToolConfiguration tool, final FilteredLog log) {
        var delegate = new FileSystemCoverageReportFactory();
        var report = delegate.create(tool, log);

        var metricName = delegate.extractMetric(tool, report);
        var metric = Metric.fromTag(metricName);
        log.logInfo("-> %s: %s",
                metricName, report.getValue(metric).orElse(Value.nullObject(metric)));

        return report;
    }
}
