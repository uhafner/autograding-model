package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.TestCase;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class TestMarkdown extends ScoreMarkdown<TestScore, TestConfiguration> {
    static final String TYPE = "Unit Tests Score";
    static final int MAX_LENGTH_DETAILS = 65_535 - 500;
    static final String TRUNCATED_MESSAGE = "\\[.. truncated ..\\]";

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public TestMarkdown() {
        super(TYPE, "vertical_traffic_light");
    }

    @Override
    protected List<TestScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getTestScores();
    }

    @Override
    protected void createSpecificDetails(final AggregatedScore aggregation,
            final List<TestScore> scores, final StringBuilder details) {
        for (TestScore score : scores) {
            var configuration = score.getConfiguration();
            details.append(getTitle(score));

            details.append(formatColumns("Name", "Passed", "Skipped", "Failed", "Total", "Impact"));
            details.append(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"));
            score.getSubScores().forEach(subScore -> details.append(formatColumns(
                    subScore.getName(),
                    String.valueOf(subScore.getPassedSize()),
                    String.valueOf(subScore.getSkippedSize()),
                    String.valueOf(subScore.getFailedSize()),
                    String.valueOf(subScore.getTotalSize()),
                    String.valueOf(subScore.getImpact()))));

            if (score.getSubScores().size() > 1) {
                details.append(formatBoldColumns("Total",
                        sum(aggregation, TestScore::getPassedSize),
                        sum(aggregation, TestScore::getSkippedSize),
                        sum(aggregation, TestScore::getFailedSize),
                        sum(aggregation, TestScore::getTotalSize),
                        sum(aggregation, TestScore::getImpact)));
            }
            details.append(formatItalicColumns(IMPACT,
                    renderImpact(configuration.getPassedImpact()),
                    renderImpact(configuration.getSkippedImpact()),
                    renderImpact(configuration.getFailureImpact()),
                    TOTAL,
                    LEDGER));

            if (score.hasFailures()) {
                details.append("### Failures\n");
                score.getFailures().forEach(issue -> appendReasonForFailure(details, issue));
                details.append("\n");
            }
        }
    }

    @Override
    protected void createSpecificSummary(final List<TestScore> scores, final StringBuilder summary) {
        for (TestScore score : scores) {
            summary.append(getTitle(score));
            if (score.hasFailures()) {
                summary.append(String.format("%d tests failed, %d passed", score.getFailedSize(), score.getPassedSize()));
            }
            else {
                summary.append(String.format("%d tests passed", score.getPassedSize()));
            }
            if (score.getSkippedSize() > 0) {
                summary.append(String.format(", %d skipped", score.getSkippedSize()));
            }
            summary.append("\n");
        }
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
        return String.format("__%s:%s__%n"
                + getMessage(issue)
                + "<details>%n"
                + "<summary>Stack Trace</summary>"
                + "%n%n"
                + "```text%n"
                + "%s%n"
                + "```"
                + "%n"
                + "</details>%n%n", issue.getClassName(), issue.getTestName(), issue.getDescription());
    }

    private String getMessage(final TestCase issue) {
        if (issue.getMessage().isBlank()) {
            return StringUtils.EMPTY;
        }
        return issue.getMessage() + "%n";
    }
}
