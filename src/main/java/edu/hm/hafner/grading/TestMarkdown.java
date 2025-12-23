package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.function.Function;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public class TestMarkdown extends ScoreMarkdown<TestScore, TestConfiguration> {
    static final String TYPE = "Test Score";
    static final String JUNIT_ICON = "<img src=\"https://junit.org/junit5/assets/img/junit5-logo.png\" alt=\"JUnit\" height=\"18\" width=\"18\">";
    private static final String TRUNCATION_TEXT = "\n\nToo many test failures. Grading output truncated.\n\n";

    /**
     * Creates a new Markdown renderer for test results.
     */
    public TestMarkdown() {
        super(TYPE, JUNIT_ICON);
    }

    @Override
    protected List<TestScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getTestScores();
    }

    @Override
    @SuppressWarnings("checkstyle:LambdaBodyLength")
    protected String createSpecificDetails(final List<TestScore> scores) {
        var total = new StringBuilder();
        for (TestScore score : scores) {
            var details = new TruncatedStringBuilder().withTruncationText(TRUNCATION_TEXT);
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addTextIf(formatColumns("Icon", "Name", "Scope", "Tests", "Success %"),
                            score.getConfiguration().isRelative())
                    .addTextIf(formatColumns("Icon", "Name", "Scope", "Tests", "Passed", "Skipped", "Failed"),
                            !score.getConfiguration().isRelative())
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addText(formatColumns("Status"))
                    .addNewline()
                    .addTextIf(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:"),
                            score.getConfiguration().isRelative())
                    .addTextIf(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"),
                            !score.getConfiguration().isRelative())
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addText(formatColumns(":-:"))
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(
                            getIcon(subScore),
                            subScore.getName(),
                            subScore.getScope().getDisplayName(),
                            String.valueOf(subScore.getTotalSize())
                    ))
                    .addTextIf(formatColumns(
                                    String.valueOf(subScore.getSuccessRate())),
                            score.getConfiguration().isRelative())
                    .addTextIf(formatColumns(
                                    String.valueOf(subScore.getPassedSize()),
                                    String.valueOf(subScore.getSkippedSize()),
                                    String.valueOf(subScore.getFailedSize())),
                            !score.getConfiguration().isRelative())
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addText(formatColumns(getSuccessIcon(!subScore.hasFailures())))
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addTextIf(formatBoldColumns("Total", EMPTY, EMPTY,
                                        sum(score, TestScore::getTotalSize),
                                        score.getSuccessRate()),
                                score.getConfiguration().isRelative())
                        .addTextIf(formatBoldColumns("Total", EMPTY, EMPTY,
                                        sum(score, TestScore::getTotalSize),
                                        sum(score, TestScore::getPassedSize),
                                        sum(score, TestScore::getSkippedSize),
                                        sum(score, TestScore::getFailedSize)),
                                !score.getConfiguration().isRelative())
                        .addTextIf(formatBoldColumns(sum(score, TestScore::getImpact)), score.hasMaxScore())
                        .addText(formatBoldColumns(EMPTY))
                        .addNewline();
            }

            if (score.hasSkippedTests()) {
                addTestDetails(details, "### Skipped Tests", score.getSkippedTests(), this::renderTest);
            }

            if (score.hasFailures()) {
                var failuresWithStackTrace = getFailedTests(score, scores.size(), this::renderFailure);
                if (failuresWithStackTrace.contains(TRUNCATION_TEXT)) { // retry and render only failed tests
                    details.addText(getFailedTests(score, scores.size(), this::renderTest));
                }
                else {
                    details.addText(failuresWithStackTrace);
                }
            }

            details.addNewline();
            total.append(details);
        }
        return total.toString();
    }

    private String getFailedTests(final TestScore score, final int size, final Function<TestCase, String> renderer) {
        var builder = new TruncatedStringBuilder().withTruncationText(TRUNCATION_TEXT);
        addTestDetails(builder, "### Failures", score.getFailures(), renderer);
        return builder.build().buildByChars(MARKDOWN_MAX_SIZE / size);
    }

    private void addTestDetails(final TruncatedStringBuilder details,
            final String title, final List<TestCase> testCases, final Function<TestCase, String> renderer) {
        details.addNewline().addText(title).addNewline();
        testCases.stream()
                .map(renderer)
                .map(s -> LINE_BREAK + s)
                .forEach(details::addText);
    }

    private String renderTest(final TestCase issue) {
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
    protected String getToolIcon(final TestScore score) {
        return getDefaultIcon(score); // no customizations for test scores
    }

    private String getSuccessIcon(final boolean successful) {
        return successful ? CHECK : CROSS;
    }
}
