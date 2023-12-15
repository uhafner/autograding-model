package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class TestMarkdown extends ScoreMarkdown<TestScore, TestConfiguration> {
    static final String TYPE = "Unit Tests Score";

    /**
     * Creates a new Markdown renderer for test results.
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
            final List<TestScore> scores, final TruncatedStringBuilder details) {
        for (TestScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addNewline()
                    .addText(formatColumns("Name", "Passed", "Skipped", "Failed", "Total", "Impact"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(
                            subScore.getName(),
                            String.valueOf(subScore.getPassedSize()),
                            String.valueOf(subScore.getSkippedSize()),
                            String.valueOf(subScore.getFailedSize()),
                            String.valueOf(subScore.getTotalSize()),
                            String.valueOf(subScore.getImpact())))
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total",
                                sum(aggregation, TestScore::getPassedSize),
                                sum(aggregation, TestScore::getSkippedSize),
                                sum(aggregation, TestScore::getFailedSize),
                                sum(aggregation, TestScore::getTotalSize),
                                sum(aggregation, TestScore::getImpact)))
                        .addNewline();
            }

            var configuration = score.getConfiguration();
            details.addText(formatColumns(IMPACT))
                    .addText(formatItalicColumns(
                            renderImpact(configuration.getPassedImpact()),
                            renderImpact(configuration.getSkippedImpact()),
                            renderImpact(configuration.getFailureImpact())))
                    .addText(formatColumns(TOTAL, LEDGER))
                    .addNewline();

            if (score.hasSkippedTests()) {
                details.addText("### Skipped Test Cases\n");
                score.getSkippedTests().stream()
                        .map(this::renderSkippedTest)
                        .forEach(details::addText);
                details.addNewline();
            }

            if (score.hasFailures()) {
                details.addText("### Failures\n");
                score.getFailures().stream()
                        .map(this::renderFailure)
                        .forEach(details::addText);
                details.addNewline();
            }
        }
    }

    private String renderSkippedTest(final TestCase issue) {
        return String.format("- %s#%s%n", issue.getClassName(), issue.getTestName());
    }

    private String renderFailure(final TestCase issue) {
        return String.format("__%s:%s__", issue.getClassName(), issue.getTestName())
                + LINE_BREAK
                + getMessage(issue)
                + String.format("""
                        <details>
                          <summary>Stack Trace</summary>
                        
                          ```text
                          %s
                          ```
                          
                        </details> 
                        """,
                issue.getDescription())
                + LINE_BREAK;
    }

    private String getMessage(final TestCase issue) {
        if (issue.getMessage().isBlank()) {
            return StringUtils.EMPTY;
        }
        return issue.getMessage() + LINE_BREAK;
    }

    @Override
    protected void createSpecificSummary(final TestScore score, final StringBuilder summary) {
        if (score.hasFailures()) {
            summary.append(
                    String.format("%d tests failed, %d passed", score.getFailedSize(), score.getPassedSize()));
        }
        else {
            summary.append(String.format("%d tests passed", score.getPassedSize()));
        }
        if (score.getSkippedSize() > 0) {
            summary.append(String.format(", %d skipped", score.getSkippedSize()));
        }
    }

    private int sum(final AggregatedScore score, final Function<TestScore, Integer> property) {
        return score.getTestScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
