package edu.hm.hafner.grading;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Creates a human-readable report of the grading results.
 *
 * @author Tobias Effner
 */
public class GradingReport {

    private static final TestMarkdown TEST_MARKDOWN = new TestMarkdown();
    private static final AnalysisMarkdown ANALYSIS_MARKDOWN = new AnalysisMarkdown();
    private static final CodeCoverageMarkdown CODE_COVERAGE_MARKDOWN = new CodeCoverageMarkdown();
    private static final MutationCoverageMarkdown MUTATION_COVERAGE_MARKDOWN = new MutationCoverageMarkdown();

    /**
     * Returns a short header for the grading results, this value typically will be used as link name.
     *
     * @return the header (plain ASCII text)
     */
    public String getHeader() {
        return "Autograding results";
    }

    /**
     * Returns a short summary for the grading results. This text does not use Markdown and fits into a single line.
     *
     * @param score
     *         the aggregated score
     *
     * @return the summary (plain ASCII text)
     */
    public String getTextSummary(final AggregatedScore score) {
        var summary = new StringJoiner(", ", " (", ")");
        summary.setEmptyValue(StringUtils.EMPTY);

        if (score.hasTests()) {
            summary.add(String.format("unit tests: %d/%d", score.getTestAchievedScore(),
                    score.getTestMaxScore()));
        }
        if (score.hasCodeCoverage()) {
            summary.add(String.format("code coverage: %d/%d", score.getCodeCoverageAchievedScore(),
                    score.getCodeCoverageMaxScore()));
        }
        if (score.hasMutationCoverage()) {
            summary.add(String.format("mutation coverage: %d/%d", score.getMutationCoverageAchievedScore(),
                    score.getMutationCoverageMaxScore()));
        }
        if (score.hasAnalysis()) {
            summary.add(String.format("analysis: %d/%d", score.getAnalysisAchievedScore(),
                    score.getAnalysisMaxScore()));
        }
        return String.format(
                "Total score - %d of %d%s",
                score.getAchievedScore(), score.getMaxScore(),
                summary);
    }

    /**
     * Creates a summary of the grading results in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param title
     *         the title of the summary
     *
     * @return Markdown text
     */
    public String getMarkdownSummary(final AggregatedScore score, final String title) {
        var summary = new StringBuilder();

        summary.append(createTotal(score, title));

        if (score.hasTests()) {
            summary.append(TEST_MARKDOWN.createSummary(score));
        }
        if (score.hasCodeCoverage()) {
            summary.append(CODE_COVERAGE_MARKDOWN.createSummary(score));
        }
        if (score.hasMutationCoverage()) {
            summary.append(MUTATION_COVERAGE_MARKDOWN.createSummary(score));
        }
        if (score.hasAnalysis()) {
            summary.append(ANALYSIS_MARKDOWN.createSummary(score));
        }

        return summary.toString();
    }

    /**
     * Creates a detailed description of the grading results in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return Markdown text
     */
    public String getMarkdownDetails(final AggregatedScore score) {
        return createTotal(score, ":mortar_board: Total score")
                + TEST_MARKDOWN.createDetails(score)
                + ANALYSIS_MARKDOWN.createDetails(score)
                + CODE_COVERAGE_MARKDOWN.createDetails(score)
                + MUTATION_COVERAGE_MARKDOWN.createDetails(score);
    }

    private String createTotal(final AggregatedScore score, final String title) {
        return String.format("# %s - %s of %s%n",
                title, score.getAchievedScore(), score.getMaxScore());
    }

    /**
     * Creates an error message in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param exception
     *         the exception that caused the error
     *
     * @return Markdown text
     */
    public String getMarkdownErrors(final AggregatedScore score, final Throwable exception) {
        return String.format(
                "# Partial score: %s/%s%n:construction: The grading has been aborted due to an error.%n",
                score.getAchievedScore(), score.getMaxScore())
                + createExceptionSection(exception)
                + createLogSection(score);

    }

    private String createExceptionSection(final Throwable exception) {
        return String.format("## Exception%n```%n%s%n```%n", ExceptionUtils.getStackTrace(exception));
    }

    private String createLogSection(final AggregatedScore score) {
        return String.format("## Error Messages%n```%n%s%n```%n## Information Messages%n```%n%s%n```%n",
                joinMessages(score.getErrorMessages()),
                joinMessages(score.getInfoMessages()));
    }

    private String joinMessages(final List<String> messages) {
        return messages.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
