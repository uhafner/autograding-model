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
                    .addText(getPercentageImage(score))
                    .addNewline()
                    .addText(formatColumns("Name", "Reports", "Passed", "Skipped", "Failed", "Total"))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(
                            subScore.getName(),
                            String.valueOf(subScore.getReportFiles()),
                            String.valueOf(subScore.getPassedSize()),
                            String.valueOf(subScore.getSkippedSize()),
                            String.valueOf(subScore.getFailedSize()),
                            String.valueOf(subScore.getTotalSize())))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total",
                                sum(score, TestScore::getReportFiles),
                                sum(score, TestScore::getPassedSize),
                                sum(score, TestScore::getSkippedSize),
                                sum(score, TestScore::getFailedSize),
                                sum(score, TestScore::getTotalSize)))
                        .addTextIf(formatBoldColumns(sum(score, TestScore::getImpact)), score.hasMaxScore())
                        .addNewline();
            }

            var configuration = score.getConfiguration();
            if (score.hasMaxScore()) {
                details.addText(formatColumns(IMPACT, EMPTY))
                        .addText(formatItalicColumns(
                                renderImpact(configuration.getPassedImpact()),
                                renderImpact(configuration.getSkippedImpact()),
                                renderImpact(configuration.getFailureImpact())))
                        .addText(formatColumns(TOTAL, LEDGER))
                        .addNewline();
            }

            if (score.hasSkippedTests()) {
                details.addText("### Skipped Test Cases")
                        .addNewline();
                score.getSkippedTests().stream()
                        .map(this::renderSkippedTest)
                        .forEach(details::addText);
                details.addNewline();
            }

            if (score.hasFailures()) {
                details.addText("### Failures").addNewline();
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

    private int sum(final TestScore score, final Function<TestScore, Integer> property) {
        return score.getSubScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
