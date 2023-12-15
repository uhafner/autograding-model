package edu.hm.hafner.grading;

import java.util.List;
import java.util.stream.Collectors;

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
    private static final String DEFAULT_TITLE = "Autograding score";

    /**
     * Returns a short summary for the grading results. This text does not use Markdown and fits into a single line.
     *
     * @param score
     *         the aggregated score
     *
     * @return the summary (plain ASCII text)
     */
    public String getTextSummary(final AggregatedScore score) {
        return getTextSummary(score, DEFAULT_TITLE);
    }

    /**
     * Returns a short summary for the grading results. This text does not use Markdown and fits into a single line.
     *
     * @param score
     *         the aggregated score
     * @param title
     *         the title to use in the summary
     *
     * @return the summary (plain ASCII text)
     */
    public String getTextSummary(final AggregatedScore score, final String title) {
        return createTotal(score, title);
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

        summary.append(createMarkdownTotal(score, title, 3));

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
        return createMarkdownTotal(score, DEFAULT_TITLE, 1)
                + "\n\n"
                + TEST_MARKDOWN.createDetails(score)
                + ANALYSIS_MARKDOWN.createDetails(score)
                + CODE_COVERAGE_MARKDOWN.createDetails(score)
                + MUTATION_COVERAGE_MARKDOWN.createDetails(score);
    }

    private String createMarkdownTotal(final AggregatedScore score, final String title, final int size) {
        return "#".repeat(size) + " :mortar_board: " + createTotal(score, title);
    }

    private String createTotal(final AggregatedScore score, final String title) {
        return String.format("%s - %s of %s", title, score.getAchievedScore(), score.getMaxScore());
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
