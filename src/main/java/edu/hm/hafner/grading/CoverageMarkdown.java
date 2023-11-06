package edu.hm.hafner.grading;

import java.util.function.Function;

/**
 * Renders the coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class CoverageMarkdown extends ScoreMarkdown {
    static final String TYPE = "Coverage Score";

    private final String coveredText;
    private final String missedText;

    /**
     * Creates a new Markdown renderer for code coverage results.
     *
     * @param coveredText
     *         the text to use for the covered column
     * @param missedText
     *         the text to use for the missed column
     */
    public CoverageMarkdown(final String coveredText, final String missedText) {
        super(TYPE, "paw_prints");

        this.coveredText = coveredText;
        this.missedText = missedText;
    }

    /**
     * Creates a new Markdown renderer for code coverage results.
     */
    public CoverageMarkdown() {
        this("Covered %", "Missed %");
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
        var scores = aggregation.getCoverageScores();
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

    private int sum(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return score.getCoverageScores().stream().map(property).reduce(Integer::sum).orElse(0);
    }

    private int average(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return sum(score, property) / score.getCoverageScores().size();
    }
}
