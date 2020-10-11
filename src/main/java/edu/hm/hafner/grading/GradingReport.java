package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;

/**
 * Creates an human readable report of the grading results.
 *
 * @author Tobias Effner
 */
public class GradingReport {
    /**
     * Creates a summary comment in Markdown that shows the aggregated results.
     *
     * @param score
     *         the aggregated score
     * @param testReports
     *         JUnit reports that many contain details about failed tests
     * @param analysisReports
     *         static analysis reports with warnings
     *
     * @return comment (formatted with Markdown)
     */
    public String getDetails(final AggregatedScore score, final List<Report> testReports,
            final List<Report> analysisReports) {
        StringBuilder comment = new StringBuilder();
        comment.append(String.format("# Total score: %s/%s%n", score.getAchieved(), score.getTotal()));

        TestMarkdown testWriter = new TestMarkdown();
        comment.append(testWriter.create(score, testReports));

        AnalysisMarkdown analysisMarkdown = new AnalysisMarkdown();
        comment.append(analysisMarkdown.create(score, analysisReports));

        CoverageMarkdown coverageWriter = new CoverageMarkdown();
        comment.append(coverageWriter.create(score));

        PitMarkdown pitWriter = new PitMarkdown();
        comment.append(pitWriter.create(score));

        return comment.toString();
    }

    /**
     * Returns a short header for the grading results, this value typically will be used as link name.
     *
     * @return the header (plain ASCII text)
     */
    public String getHeader() {
        return "Autograding results";
    }

    /**
     * Returns a short summary for the grading results, this value does not support Markdown and should fit into a
     * single line.
     *
     * @param score
     *         the aggregated score
     *
     * @return the summary (plain ASCII text)
     */
    public String getSummary(final AggregatedScore score) {
        return String.format(
                "Total score: %d/%d (unit tests: %d/%d, code coverage: %d/%d, mutation coverage: %d/%d, analysis: %d/%d)",
                score.getAchieved(), score.getTotal(),
                score.getTestAchieved(), score.getTestConfiguration().getMaxScore(),
                score.getCoverageAchieved(), score.getCoverageConfiguration().getMaxScore(),
                score.getPitAchieved(), score.getPitConfiguration().getMaxScore(),
                score.getAnalysisAchieved(), score.getAnalysisConfiguration().getMaxScore());
    }
}
