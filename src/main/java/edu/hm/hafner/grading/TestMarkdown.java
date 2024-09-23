package edu.hm.hafner.grading;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        return format("- %s#%s%n", issue.getClassName(), issue.getTestName());
    }

    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE",
            justification = "Output is Unix anyway")
    private String renderFailure(final TestCase issue) {
        return format("__%s:%s__", issue.getClassName(), issue.getTestName())
                + LINE_BREAK
                + getMessage(issue)
                + format("""
                        <details>
                          <summary>Stack Trace</summary>
                        
                          ```text
                          %s
                          ```

                        </details>
                        """, issue.getDescription())
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

    @Override
    protected String createSummary(final TestScore score) {
        var summary = new StringBuilder(CAPACITY);

        summary.append(SPACE).append(SPACE)
                .append(getTitle(score, 0));
        if (score.hasFailures() || score.hasPassedTests() || score.hasSkippedTests()) {
            summary.append(": ").append(
                    format("%2d %% successful", Math.round(score.getPassedSize() * 100.0 / score.getTotalSize())));
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
            summary.append(joiner).append(LINE_BREAK);
        }
        return summary.toString();
    }

    /*
### :sunny: &nbsp; Quality Monitor

---
<img title="Code Coverage: 73%" width="110" height="110"
        align="left" alt="Code Coverage: 93%"
        src="https://raw.githubusercontent.com/uhafner/autograding-model/main/percentages/073.svg" />

- :vertical_traffic_light: &nbsp; Tests: 291 tests passed
   - ✔️ 291 passed
   - ❌ 2 failed
   - 🙈 4 ignored
&nbsp;
---
<img title="Code Coverage: 93%" width="110" height="110"
        align="left" alt="Code Coverage: 93%"
        src="https://raw.githubusercontent.com/uhafner/autograding-model/main/percentages/093.svg" />

- :footprints: &nbsp; Code Coverage: 96% coverage achieved
  - 〰️ 70 % lines covered (7 missed)
  - ➰ 80 % branches covered (10 missed)
  - 〽️ 45 complexity


---
- :microscope: &nbsp; Mutation Coverage: 93% mutations killed
- ☑️ 99% Test strength
---
- :warning: &nbsp; Style:: No warnings
- :bug: &nbsp; Bugs: No warnings
<br/>

Created by [Quality Monitor](https://github.com/uhafner/quality-monitor/releases/tag/v1.6.0) v1.6.0 (#85eae94). More details are shown in the [GitHub Checks Result](https://github.com/jenkinsci/coverage-model/runs/23474192891).

     */
}
