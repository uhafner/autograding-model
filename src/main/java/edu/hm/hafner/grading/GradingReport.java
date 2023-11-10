package edu.hm.hafner.grading;

import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Report;

/**
 * Creates a human-readable report of the grading results.
 *
 * @author Tobias Effner
 */
public class GradingReport {
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
        var summary = new StringJoiner(", ", " (", ")");
        summary.setEmptyValue(StringUtils.EMPTY);

        if (score.hasTests()) {
            summary.add(String.format("unit tests: %d/%d", score.getTestAchievedScore(),
                    score.getTestMaxScore()));
        }
        if (score.hasCodeCoverage()) {
            summary.add(String.format("code coverage: %d/%d", score.getCodeCoverageAchievedScore(),
                    score.getCodeCoverageMaxScore()));
        }
        if (score.hasMutationCoverage()) {
            summary.add(String.format("mutation coverage: %d/%d", score.getMutationCoverageAchievedScore(),
                    score.getMutationCoverageMaxScore()));
        }
        if (score.hasAnalysis()) {
            summary.add(String.format("analysis: %d/%d", score.getAnalysisAchievedScore(),
                    score.getAnalysisMaxScore()));
        }
        return String.format(
                "Total score: %d/%d%s",
                score.getAchievedScore(), score.getMaxScore(),
                summary);
    }

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

        return String.format("# Total score: %s/%s%n",
                score.getAchievedScore(), score.getMaxScore())
                + new TestMarkdown().create(score)
                + new AnalysisMarkdown().create(score)
                + new CodeCoverageMarkdown().create(score)
                + new MutationCoverageMarkdown().create(score);
    }
}
