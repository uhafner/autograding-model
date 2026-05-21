package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;

import java.util.NoSuchElementException;

/**
 * Factory to create the static analysis reports based on the analysis-model.
 *
 * @see <a href="https://github.com/jenkinsci/analysis-model">Analysis Model</a>
 */
interface ToolParser {
    /**
     * Creates a static analysis report for the specified tool.
     *
     * @param tool
     *         the tool to create the report for
     * @param directory
     *         the directory to scan for reports
     * @param excluded
     *         the excluded files
     * @param log
     *         the logger to report the progress
     *
     * @return the created report
     * @throws NoSuchElementException
     *         if there is no analysis report for the specified tool
     */
    Report readReport(ToolConfiguration tool, String directory, String excluded, FilteredLog log);

    /**
     * Creates a coverage report for the specified tool.
     *
     * @param configuration
     *         the configuration to create the report for
     * @param directory
     *         the directory to scan for reports
     * @param excluded
     *         the excluded files
     * @param log
     *         the logger to report the progress
     *
     * @return the created report
     * @throws NoSuchElementException
     *         if there is no coverage report for the specified tool
     */
    Node readNode(ToolConfiguration configuration, String directory, String excluded, FilteredLog log);
}
