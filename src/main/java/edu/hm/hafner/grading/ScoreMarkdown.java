package edu.hm.hafner.grading;

import java.util.List;

/**
 * Base class to render results in Markdown.
 *
 * @author Ullrich Hafner
 * @param <S> the {@link Score} type
 * @param <C> the associated {@link Configuration} type
 */
abstract class ScoreMarkdown<S extends Score<S, C>, C extends Configuration> {
    static final String LEDGER = ":ledger:";
    static final String IMPACT = ":moneybag:";
    static final String N_A = "-";

    static final int MESSAGE_INITIAL_CAPACITY = 1024;

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

        var details = new StringBuilder(MESSAGE_INITIAL_CAPACITY);
        createSpecificDetails(aggregation, scores, details);
        return details.toString();
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
    protected abstract void createSpecificDetails(AggregatedScore aggregation, List<S> scores, StringBuilder details);

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
        createSpecificSummary(scores, summary);
        return summary.toString();
    }

    /**
     * Renders the score summary of the specific scores in Markdown.
     *
     * @param scores
     *         the scores to render the summary for
     * @param summary
     *         the summary Markdown
     */
    protected abstract void createSpecificSummary(List<S> scores, StringBuilder summary);

    /**
         * Creates the scores to render.
         *
         * @param aggregation
         *         the aggregated score
         *
         * @return the scores
         */
    protected abstract List<S> createScores(AggregatedScore aggregation);

    String getNotEnabled() {
        return getTitle(" not enabled");
    }

    String getNotFound() {
        return String.format("## :construction: %s enabled but no results found :construction:%n", type);
    }

    String getSummary(final int score, final int total) {
        return getTitle(String.format(": %d of %d", score, total));
    }

    protected String getTitle(final String message) {
        return getTitle(message, type);
    }

    protected String getTitle(final String message, final String name) {
        return String.format("## :%s: %s%s :%s:%n", icon, name, message, icon);
    }

    protected String getTitle(final Score<?, ?> score) {
        return getTitle(String.format(": %d of %d", score.getValue(), score.getMaxScore()), score.getName());
    }

    String formatColumns(final Object... columns) {
        return format("|%s", columns);
    }

    String formatItalicColumns(final Object... columns) {
        return format("|*%s*", columns);
    }

    String formatBoldColumns(final Object... columns) {
        return format("|**%s**", columns);
    }

    private String format(final String format, final Object... columns) {
        var row = new StringBuilder(MESSAGE_INITIAL_CAPACITY);
        for (Object column : columns) {
            row.append(String.format(format, column));
        }
        row.append('\n');
        return row.toString();
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
        return getTitle(": not enabled");
    }
}
