package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

/**
 * Base class to render results in Markdown.
 *
 * @param <S>
 *         the {@link Score} type
 * @param <C>
 *         the associated {@link Configuration} type
 *
 * @author Ullrich Hafner
 */
abstract class ScoreMarkdown<S extends Score<S, C>, C extends Configuration> {
    static final String LINE_BREAK = "\n";
    static final String LEDGER = ":heavy_minus_sign:";
    static final String IMPACT = ":moneybag:";
    static final String TOTAL = ":heavy_minus_sign:";

    static final String N_A = "-";

    static final int MESSAGE_INITIAL_CAPACITY = 1024;
    private static final int MAX_SIZE = 10_000; // limit the size of the output to this number of characters
    private static final String TRUNCATION_TEXT = "\n\nToo many test failures. Grading output truncated.";

    private final String type;
    private final String icon;

    ScoreMarkdown(final String type, final String icon) {
        this.type = type;
        this.icon = icon;
    }

    /**
     * Renders the score details in Markdown.
     *
     * @param aggregation
     *         aggregated score
     *
     * @return formatted Markdown
     */
    public String createDetails(final AggregatedScore aggregation) {
        var scores = createScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled();
        }

        var details = new TruncatedStringBuilder().withTruncationText(TRUNCATION_TEXT);
        createSpecificDetails(aggregation, scores, details);
        return details.build().buildByChars(MAX_SIZE);
    }

    /**
     * Renders the score details of the specific scores in Markdown.
     *
     * @param aggregation
     *         aggregated score
     * @param scores
     *         the scores to render the details for
     * @param details
     *         the details Markdown
     */
    protected abstract void createSpecificDetails(AggregatedScore aggregation, List<S> scores, TruncatedStringBuilder details);

    /**
     * Renders the test results in Markdown.
     *
     * @param aggregation
     *         Aggregated score
     *
     * @return returns formatted string
     */
    public String createSummary(final AggregatedScore aggregation) {
        var scores = createScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled();
        }

        var summary = new StringBuilder(MESSAGE_INITIAL_CAPACITY);
        for (S score : scores) {
            summary.append("-")
                    .append(getTitle(score, 0))
                    .append(": ")
                    .append(score.createSummary())
                    .append(LINE_BREAK);
        }
        return summary.toString();
    }

    /**
     * Creates the scores to render.
     *
     * @param aggregation
     *         the aggregated score
     *
     * @return the scores
     */
    protected abstract List<S> createScores(AggregatedScore aggregation);

    protected String getTitle(final S score, final int size) {
        return "#".repeat(size)
                + String.format(" :%s: %s", getIcon(score), score.getName())
                + createScoreTitle(score);
    }

    private String createScoreTitle(final S score) {
        if (score.hasMaxScore()) {
            return String.format(" - %d of %d", score.getValue(), score.getMaxScore());
        }
        return StringUtils.EMPTY;
    }

    private String getIcon(final S score) {
        return StringUtils.defaultIfBlank(score.getConfiguration().getIcon(), icon);
    }

    String formatColumns(final Object... columns) {
        return format(i -> i, columns);
    }

    String formatItalicColumns(final Object... columns) {
        return format(s -> "*" + s + "*", columns);
    }

    String formatBoldColumns(final Object... columns) {
        return format(s -> "**" + s + "**", columns);
    }

    private String format(final Function<String, String> textFormatter, final Object... columns) {
        return Arrays.stream(columns)
                .map(Object::toString)
                .map(textFormatter)
                .collect(Collectors.joining("|", "|", ""));
    }

    protected String renderImpact(final int impact) {
        if (impact == 0) {
            return N_A;
        }
        else {
            return String.valueOf(impact);
        }
    }

    protected String createNotEnabled() {
        return String.format("## :%s: %s%s %n", icon, type, ": not enabled");
    }
}
