package edu.hm.hafner.grading;

import java.util.function.Function;

import edu.hm.hafner.coverage.TestCase;

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
     * @param aggregation
     *         Aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore aggregation) {
        var scores = aggregation.getTestScores();
        if (scores.isEmpty()) {
            return getTitle(": not enabled");
        }

        var comment = new StringBuilder(MESSAGE_INITIAL_CAPACITY);

        for (TestScore score : scores) {
            var configuration = score.getConfiguration();
            comment.append(getTitle(String.format(": %d of %d", score.getValue(), score.getMaxScore()), score.getName()));
            comment.append(formatColumns("Name", "Passed", "Skipped", "Failed", "Impact"));
            comment.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:"));
            score.getSubScores().forEach(subScore -> comment.append(formatColumns(
                    subScore.getName(),
                    String.valueOf(subScore.getPassedSize()),
                    String.valueOf(subScore.getSkippedSize()),
                    String.valueOf(subScore.getFailedSize()),
                    String.valueOf(subScore.getImpact()))));
            if (score.getSubScores().size() > 1) {
                comment.append(formatBoldColumns("Total",
                        sum(aggregation, TestScore::getPassedSize),
                        sum(aggregation, TestScore::getSkippedSize),
                        sum(aggregation, TestScore::getFailedSize),
                        sum(aggregation, TestScore::getImpact)));
            }
            comment.append(formatItalicColumns(IMPACT,
                    renderImpact(configuration.getPassedImpact()),
                    renderImpact(configuration.getSkippedImpact()),
                    renderImpact(configuration.getFailureImpact()),
                    LEDGER));

            if (score.hasFailures()) {
                comment.append("### Failures\n");
                score.getFailures().forEach(issue -> appendReasonForFailure(comment, issue));
                comment.append("\n");
            }

        }

        return comment.toString();

    }

    private int sum(final AggregatedScore score, final Function<TestScore, Integer> property) {
        return score.getTestScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    private void appendReasonForFailure(final StringBuilder stringBuilder, final TestCase issue) {
        var nextFailure = renderFailure(issue);
        if (stringBuilder.length() + nextFailure.length() < MAX_LENGTH_DETAILS) {
            stringBuilder.append(nextFailure);
        }
        else if (!stringBuilder.toString().endsWith(TRUNCATED_MESSAGE)) {
            stringBuilder.append(TRUNCATED_MESSAGE);
        }
    }

    private String renderFailure(final TestCase issue) {
        return String.format("<details>%n"
                + "<summary>%s:%s</summary>"
                + "%n%n"
                + "```text%n"
                + "%s%n"
                + "```"
                + "%n"
                + "</details>%n", issue.getClassName(), issue.getMessage(), issue.getDescription());
    }
}
