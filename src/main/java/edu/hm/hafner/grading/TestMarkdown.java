package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
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
    String createScoreSummary(final TestScore testScore) {
        if (!testScore.hasTests()) {
            return "No test results available";
        }

        return createTitle(testScore) + createDescription(testScore);
    }

    private String createTitle(final TestScore testScore) {
        if (testScore.hasMaxScore()) {
            return format("%s successful", testScore.getSuccessPercentage().asText(Locale.ENGLISH));
        }
        else {
            if (testScore.hasFailures()) {
                return "❌&nbsp;unstable";
            }
            return "✅&nbsp;successful";
        }
    }

    private String createDescription(final TestScore testScore) {
        var joiner = new StringJoiner(", ", " &mdash; ", "");
        if (testScore.hasDelta()) {
            if (testScore.hasFailures()) {
                joiner.add(format("%s failed %s", testScore.getFailedSize(),
                        delta(testScore.getFailedSizeDelta(), false)));
            }
            if (testScore.hasPassedTests()) {
                joiner.add(format("%s passed %s", testScore.getPassedSize(),
                        delta(testScore.getPassedSizeDelta(), true)));
            }
            if (testScore.hasSkippedTests()) {
                joiner.add(format("%s skipped %s", testScore.getSkippedSize(),
                        delta(testScore.getSkippedSizeDelta(), false)));
            }
        }
        else {
            if (testScore.hasFailures()) {
                joiner.add(format("%s failed", testScore.getFailedSize()));
            }
            if (testScore.hasPassedTests()) {
                joiner.add(format("%s passed", testScore.getPassedSize()));
            }
            if (testScore.hasSkippedTests()) {
                joiner.add(format("%s skipped", testScore.getSkippedSize()));
            }
        }
        return joiner.toString();
    }

    @Override
    @SuppressWarnings("checkstyle:LambdaBodyLength")
    protected String createSpecificDetails(final List<TestScore> scores) {
        var total = new StringBuilder();
        for (TestScore score : scores) {
            var details = new TruncatedStringBuilder().withTruncationText(TRUNCATION_TEXT);
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(formatColumns("Icon", "Name", "Scope", "Passed", "Skipped", "Failed"))
                    .addTextIf(formatColumns("Success %", "Impact"), score.hasMaxScore())
                    .addText(formatColumns("Status"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:", ":-:"), score.hasMaxScore())
                    .addText(formatColumns(":-:"))
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(
                            getIcon(subScore),
                            subScore.getName(),
                            subScore.getScope().getDisplayName(),
                            deltaCell(subScore.hasDelta(), subScore.getPassedSize(), subScore.getPassedSizeDelta(),
                                    true),
                            deltaCell(subScore.hasDelta(), subScore.getSkippedSize(), subScore.getSkippedSizeDelta(),
                                    false),
                            deltaCell(subScore.hasDelta(), subScore.getFailedSize(), subScore.getFailedSizeDelta(),
                                    false)
                    ))
                    .addTextIf(formatColumns(
                            deltaCell(subScore.hasDelta(), subScore.getSuccessRate(), subScore.getSuccessRateDelta(),
                                    true),
                            String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addText(formatColumns(getSuccessIcon(!subScore.hasFailures())))
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total", EMPTY, EMPTY,
                                deltaCell(score.hasDelta(), score.getPassedSize(), score.getPassedSizeDelta(), true),
                                deltaCell(score.hasDelta(), score.getSkippedSize(), score.getSkippedSizeDelta(), false),
                                deltaCell(score.hasDelta(), score.getFailedSize(), score.getFailedSizeDelta(), false)))
                        .addTextIf(formatBoldColumns(
                                deltaCell(score.hasDelta(), score.getSuccessRate(), score.getSuccessRateDelta(), true),
                                score.getImpact()), score.hasMaxScore())
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

    @Override
    protected String getToolIcon(final TestScore score) {
        return getDefaultIcon(score); // no customizations for test scores
    }

    private String getSuccessIcon(final boolean successful) {
        return successful ? CHECK : CROSS;
    }
}
