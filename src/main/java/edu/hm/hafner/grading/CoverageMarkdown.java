package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

/**
 * Renders the coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
abstract class CoverageMarkdown extends ScoreMarkdown {
    private final String coveredText;
    private final String missedText;

    CoverageMarkdown(final String type, final String icon, final String coveredText, final String missedText) {
        super(type, icon);

        this.coveredText = coveredText;
        this.missedText = missedText;
    }

    /**
     * Renders the code coverage results in Markdown.
     *
     * @param aggregation
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore aggregation) {
        var scores = getCoverageScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled();
        }

        var comment = new StringBuilder(MESSAGE_INITIAL_CAPACITY);

        for (CoverageScore score : scores) {
            var configuration = score.getConfiguration();
            comment.append(
                    getTitle(String.format(": %d of %d", score.getValue(), score.getMaxScore()), score.getName()));
            comment.append(formatColumns("Name", coveredText, missedText, "Impact"));
            comment.append(formatColumns(":-:", ":-:", ":-:", ":-:"));
            score.getSubScores().forEach(subScore -> comment.append(formatColumns(
                    subScore.getName(),
                    String.valueOf(subScore.getCoveredPercentage()),
                    String.valueOf(subScore.getMissedPercentage()),
                    String.valueOf(subScore.getImpact()))));
            if (score.getSubScores().size() > 1) {
                comment.append(formatBoldColumns("Total Ø",
                        score.getCoveredPercentage(),
                        score.getMissedPercentage(),
                        score.getImpact()));
            }
            comment.append(formatItalicColumns(IMPACT,
                    renderImpact(configuration.getCoveredPercentageImpact()),
                    renderImpact(configuration.getMissedPercentageImpact()),
                    LEDGER));
        }

        return comment.toString();
    }

    /**
     * Renders the test results in Markdown.
     *
     * @param aggregation
     *         Aggregated score
     *
     * @return returns formatted string
     */
    public String createSummary(final AggregatedScore aggregation) {
        var scores = getCoverageScores(aggregation);
        if (scores.isEmpty()) {
            return createNotEnabled();
        }

        var comment = new StringBuilder(MESSAGE_INITIAL_CAPACITY);

        for (CoverageScore score : scores) {
            comment.append("#");
            comment.append(getTitle(score));
            comment.append(String.format("%d%% %s, %d%% %s",
                    score.getCoveredPercentage(), getPlainText(coveredText),
                    score.getMissedPercentage(), getPlainText(missedText)));
            comment.append("\n");
        }

        return comment.toString();
    }

    private String getPlainText(final String label) {
        return label.replace("%", "");
    }

    /**
     * Returns the concrete coverage scores to render.
     *
     * @param aggregation
     *         the aggregated score
     *
     * @return the coverage scores
     */
    protected abstract List<CoverageScore> getCoverageScores(AggregatedScore aggregation);

    private int sum(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return getCoverageScores(score).stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
