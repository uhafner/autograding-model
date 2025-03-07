package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

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
                    .addParagraph()
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
                addTestDetails(details, "### Skipped Test Cases", score.getSkippedTests(), this::renderSkippedTest);
            }

            if (score.hasFailures()) {
                addTestDetails(details, "### Failures", score.getFailures(), this::renderFailure);
            }

            details.addNewline();
        }
    }

    private void addTestDetails(final TruncatedStringBuilder details,
            final String title, final List<TestCase> testCases, final Function<TestCase, String> renderer) {
        details.addNewline().addText(title).addNewline();
        testCases.stream()
                .map(renderer)
                .map(s -> LINE_BREAK + s)
                .forEach(details::addText);
    }

    private String renderSkippedTest(final TestCase issue) {
        return format("- %s#%s", issue.getClassName(), issue.getTestName());
    }

    private String renderFailure(final TestCase issue) {
        return HORIZONTAL_RULE
                + format("__%s:%s__", issue.getClassName(), issue.getTestName())
                + PARAGRAPH
                + getMessage(issue)
                + getStacktrace(issue);
    }

    private String getMessage(final TestCase issue) {
        if (issue.getMessage().isBlank()) {
            return StringUtils.EMPTY;
        }
        return format("""
                  ```text
                  %s
                  ```

                """, issue.getMessage().trim());
    }

    private String getStacktrace(final TestCase issue) {
        if (issue.getDescription().isBlank()) {
            return StringUtils.EMPTY;
        }
        return format("""
                <details>
                  <summary>Stack Trace</summary>
                
                  ```text
                  %s
                  ```
                </details>
                
                """, issue.getDescription().trim());
    }

    private int sum(final TestScore score, final Function<TestScore, Integer> property) {
        return score.getSubScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    @Override
    protected String createScoreSummary(final TestScore score) {
        if (!score.hasTests()) {
            return "No test results available";
        }
        var summary = new StringBuilder(1024);
        summary.append(format("%2d%% successful", score.getSuccessRate()));
        var joiner = new StringJoiner(", ", " (", ")");
        if (score.hasFailures()) {
            joiner.add(format(":x: %d failed", score.getFailedSize()));
        }
        if (score.hasPassedTests()) {
            joiner.add(format(":heavy_check_mark: %d passed", score.getPassedSize()));
        }
        if (score.hasSkippedTests()) {
            joiner.add(format(":see_no_evil: %d skipped", score.getSkippedSize()));
        }
        summary.append(joiner);
        return summary.toString();
    }

    @Override
    protected String getToolIcon(final TestScore score) {
        return getDefaultIcon(score); // no customizations for test scores
    }
}
