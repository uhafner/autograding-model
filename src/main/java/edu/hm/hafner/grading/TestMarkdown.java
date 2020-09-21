package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class TestMarkdown extends ScoreMarkdown {
    static final String TYPE = "Unit Tests Score";

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public TestMarkdown() {
        super(TYPE, "vertical_traffic_light");
    }

    /**
     * Renders the test results in Markdown.
     *
     * @param score
     *         Aggregated score
     * @param testReports
     *         JUnit test reports
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score, final List<Report> testReports) {
        if (!score.getTestConfiguration().isEnabled()) {
            return getNotEnabled();
        }
        if (score.getTestScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getSummary(score.getTestAchieved(), score.getTestConfiguration().getMaxScore()));
        stringBuilder.append(formatColumns(new String[] {"Passed", "Skipped", "Failed", "Impact"}));
        stringBuilder.append(formatColumns(new String[] {":-:", ":-:", ":-:", ":-:"}));
        score.getTestScores().forEach(testScore -> stringBuilder.append(formatColumns(new String[] {
                String.valueOf(testScore.getPassedSize()),
                String.valueOf(testScore.getSkippedSize()),
                String.valueOf(testScore.getFailedSize()),
                String.valueOf(testScore.getTotalImpact())})));

        if (score.hasTestFailures()) {
            stringBuilder.append("### Failures\n");
            testReports.stream().flatMap(Report::stream).forEach(
                    issue -> stringBuilder.append("- ")
                            .append(issue.getFileName())
                            .append("(")
                            .append(issue.getLineStart())
                            .append("):")
                            .append("\n```\n")
                            .append(issue.getMessage())
                            .append("\n```\n"));
        }
        
        return stringBuilder.toString();
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|\n";
        return String.format(format, columns);
    }
}
