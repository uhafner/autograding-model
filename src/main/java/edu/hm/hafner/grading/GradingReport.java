package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;

/**
 * Creates a human-readable report of the grading results.
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
     * @return comment (formatted with Markdown)
     */
    public String getDetails(final AggregatedScore score, final List<Report> testReports) {
        var testWriter = new TestMarkdown();

        var analysisMarkdown = new AnalysisMarkdown();

        var coverageWriter = new CoverageMarkdown();

        return String.format("# Total score: %s/%s%n", score.getAchievedScore(), score.getAchievedScore())
                + testWriter.create(score)
                + analysisMarkdown.create(score)
                + coverageWriter.create(score);
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
                "Total score: %d/%d (unit tests: %d/%d, code coverage: %d/%d, analysis: %d/%d)",
                score.getAchievedScore(), score.getAchievedScore(),
                score.getTestAchievedScore(), score.getTestMaxScore(),
                score.getCoverageAchievedScore(), score.getCoverageMaxScore(),
                score.getAnalysisAchievedScore(), score.getAnalysisMaxScore());
    }
}
