package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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
    private static final MetricMarkdown METRIC_MARKDOWN = new MetricMarkdown();
    private static final String DEFAULT_TITLE = "Autograding score";
    private static final String PARAGRAPH = ScoreMarkdown.PARAGRAPH;

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
     *
     * @return Markdown text
     */
    public String getMarkdownSummary(final AggregatedScore score) {
        return getMarkdownSummary(score, DEFAULT_TITLE);
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
        return getMarkdownSummary(score, title, false);
    }

    /**
     * Creates a summary of the grading results in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param title
     *         the title of the summary
     * @param showHeaders
     *         determines whether headers should be shown for the subsections or not
     *
     * @return Markdown text
     */
    public String getMarkdownSummary(final AggregatedScore score, final String title, final boolean showHeaders) {
        return createMarkdownTotal(score, title, 2) + PARAGRAPH + getSubScoreDetails(score, showHeaders)
                + ScoreMarkdown.LINE_BREAK;
    }

    private String createPercentage(final AggregatedScore score) {
        if (score.getMaxScore() == 0) {
            return StringUtils.EMPTY;
        }
        var imageSize = 150;
        return ScoreMarkdown.getPercentageImage("Score percentage", score.getAchievedPercentage(), imageSize)
                + PARAGRAPH;
    }

    /**
     * Returns a short summary for all sub scores that are part of the aggregation in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return Markdown text
     */
    public StringBuilder getSubScoreDetails(final AggregatedScore score) {
        return getSubScoreDetails(score, false);
    }

    /**
     * Returns a short summary for all sub scores that are part of the aggregation in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param showHeaders
     *         determines whether headers should be shown for the subsections or not
     *
     * @return Markdown text
     */
    public StringBuilder getSubScoreDetails(final AggregatedScore score, final boolean showHeaders) {
        var summary = new StringBuilder();

        summary.append(createPercentage(score));
        summary.append(joinSummaries(score, showHeaders));

        return summary;
    }

    private String joinSummaries(final AggregatedScore score, final boolean showHeaders) {
        var joiner = new StringJoiner(showHeaders ? ScoreMarkdown.PARAGRAPH : ScoreMarkdown.LINE_BREAK_PARAGRAPH);

        add(score, showHeaders, joiner, TEST_MARKDOWN);
        add(score, showHeaders, joiner, CODE_COVERAGE_MARKDOWN);
        add(score, showHeaders, joiner, MUTATION_COVERAGE_MARKDOWN);
        add(score, showHeaders, joiner, ANALYSIS_MARKDOWN);
        add(score, showHeaders, joiner, METRIC_MARKDOWN);

        return joiner.toString();
    }

    private void add(final AggregatedScore score, final boolean showHeaders, final StringJoiner joiner,
            final ScoreMarkdown<?, ?> markdown) {
        var summary = markdown.createSummary(score, showHeaders);
        if (!summary.isBlank()) {
            joiner.add(summary);
        }
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
        return getMarkdownDetails(score, DEFAULT_TITLE);
    }

    /**
     * Creates a detailed description of the grading results in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param title
     *         the title of the details
     *
     * @return Markdown text
     */
    public String getMarkdownDetails(final AggregatedScore score, final String title) {
        return getMarkdownDetails(score, title, false);
    }

    /**
     * Creates a detailed description of the grading results in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param title
     *         the title of the details
     * @param showDisabled
     *         determines whether disabled scores should be shown or skipped
     *
     * @return Markdown text
     */
    public String getMarkdownDetails(final AggregatedScore score, final String title, final boolean showDisabled) {
        return createMarkdownTotal(score, title, 1)
                + PARAGRAPH
                + TEST_MARKDOWN.createDetails(score, showDisabled)
                + ANALYSIS_MARKDOWN.createDetails(score, showDisabled)
                + CODE_COVERAGE_MARKDOWN.createDetails(score, showDisabled)
                + MUTATION_COVERAGE_MARKDOWN.createDetails(score, showDisabled)
                + METRIC_MARKDOWN.createDetails(score, showDisabled);
    }

    private String createMarkdownTotal(final AggregatedScore score, final String title, final int size) {
        if (score.getMaxScore() == 0) {
            return "#".repeat(size) + " :sunny: &nbsp; " + title;
        }
        return "#".repeat(size) + " :mortar_board: &nbsp; " + createTotal(score, title);
    }

    private String createTotal(final AggregatedScore score, final String title) {
        return title + ScoreMarkdown.createScoreTitleSuffix(score.getMaxScore(),
                score.getAchievedScore(), score.getAchievedPercentage());
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
        return "# Partial score: %s/%s%n:construction: The grading has been aborted due to an error.%n".formatted(
                score.getAchievedScore(), score.getMaxScore())
                + createExceptionSection(exception)
                + createLogSection(score);
    }

    private String createExceptionSection(final Throwable exception) {
        return "%n## Exception%n```%n%s%n```%n".formatted(ExceptionUtils.getStackTrace(exception));
    }

    private String createLogSection(final AggregatedScore score) {
        return "%n## Error Messages%n```%n%s%n```%n## Information Messages%n```%n%s%n```%n".formatted(
                joinMessages(score.getErrorMessages()),
                joinMessages(score.getInfoMessages()));
    }

    private String joinMessages(final List<String> messages) {
        return messages.stream().collect(Collectors.joining(System.lineSeparator()));
    }
}
