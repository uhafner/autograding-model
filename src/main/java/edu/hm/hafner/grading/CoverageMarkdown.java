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
            return getTitle(": not enabled");
        }

        var comment = new StringBuilder(MESSAGE_INITIAL_CAPACITY);

        for (CoverageScore score : scores) {
            var configuration = score.getConfiguration();
            comment.append(getTitle(String.format(": %d of %d", score.getValue(), score.getMaxScore()), score.getName()));
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

    protected abstract List<CoverageScore> getCoverageScores(AggregatedScore aggregation);

    private int sum(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return getCoverageScores(score).stream().map(property).reduce(Integer::sum).orElse(0);
    }

    private int average(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return sum(score, property) / getCoverageScores(score).size();
    }
}
