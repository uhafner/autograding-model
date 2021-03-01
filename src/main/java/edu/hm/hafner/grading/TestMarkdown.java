package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class TestMarkdown extends ScoreMarkdown {
    static final String TYPE = "Unit Tests Score";
    static final int MAX_LENGTH_DETAILS = 65_535 - 500;
    static final String TRUNCATED_MESSAGE = "\\[.. truncated ..\\]";

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
        TestConfiguration configuration = score.getTestConfiguration();
        if (!configuration.isEnabled()) {
            return getNotEnabled();
        }
        if (score.getTestScores().isEmpty()) {
            return getNotFound();
        }

        StringBuilder comment = new StringBuilder();
        comment.append(getSummary(score.getTestAchieved(), configuration.getMaxScore()));
        comment.append(formatColumns("Name", "Passed", "Skipped", "Failed", "Impact"));
        comment.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:"));
        comment.append(formatItalicColumns(IMPACT,
                renderImpact(configuration.getPassedImpact()),
                renderImpact(configuration.getSkippedImpact()),
                renderImpact(configuration.getFailureImpact()),
                createSign(configuration)
        ));
        score.getTestScores().forEach(testScore -> comment.append(formatColumns(
                testScore.getName(),
                String.valueOf(testScore.getPassedSize()),
                String.valueOf(testScore.getSkippedSize()),
                String.valueOf(testScore.getFailedSize()),
                String.valueOf(testScore.getTotalImpact()))));
        if (score.getTestScores().size() > 1) {
            comment.append(formatBoldColumns("Total",
                    sum(score, TestScore::getPassedSize),
                    sum(score, TestScore::getSkippedSize),
                    sum(score, TestScore::getFailedSize),
                    sum(score, TestScore::getTotalImpact)));
        }

        if (score.hasTestFailures()) {
            comment.append("### Failures\n");
            testReports.stream().flatMap(Report::stream).forEach(issue -> appendReasonForFailure(comment, issue));
            comment.append("\n");
        }

        return comment.toString();
    }

    private int sum(final AggregatedScore score, final Function<TestScore, Integer> property) {
        return score.getTestScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    private void appendReasonForFailure(final StringBuilder stringBuilder, final Issue issue) {
        String nextFailure = renderFailure(issue);
        if (stringBuilder.length() + nextFailure.length() < MAX_LENGTH_DETAILS) {
            stringBuilder.append(nextFailure);
        }
        else if (!stringBuilder.toString().endsWith(TRUNCATED_MESSAGE)) {
            stringBuilder.append(TRUNCATED_MESSAGE);
        }
    }

    private String renderFailure(final Issue issue) {
        return String.format("<details>%n"
                + "<summary>%s(%d)</summary>"
                + "%n%n"
                + "```text%n"
                + "%s%n"
                + "```"
                + "%n"
                + "</details>%n", issue.getFileName(), issue.getLineStart(), issue.getMessage());
    }
}
