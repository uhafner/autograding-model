package edu.hm.hafner.grading;

import java.util.List;
import java.util.function.Function;

/**
 * Renders the coverage results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
abstract class CoverageMarkdown extends ScoreMarkdown<CoverageScore, CoverageConfiguration> {
    private final String coveredText;
    private final String missedText;

    CoverageMarkdown(final String type, final String icon, final String coveredText, final String missedText) {
        super(type, icon);

        this.coveredText = coveredText;
        this.missedText = missedText;
    }

    @Override
    protected void createSpecificDetails(final AggregatedScore aggregation, final List<CoverageScore> scores,
            final StringBuilder details) {
        for (CoverageScore score : scores) {
            var configuration = score.getConfiguration();
            details.append(
                    getTitle(String.format(": %d of %d", score.getValue(), score.getMaxScore()), score.getName()));
            details.append(formatColumns("Name", coveredText, missedText, "Impact"));
            details.append(formatColumns(":-:", ":-:", ":-:", ":-:"));
            score.getSubScores().forEach(subScore -> details.append(formatColumns(
                    subScore.getName(),
                    String.valueOf(subScore.getCoveredPercentage()),
                    String.valueOf(subScore.getMissedPercentage()),
                    String.valueOf(subScore.getImpact()))));
            if (score.getSubScores().size() > 1) {
                details.append(formatBoldColumns("Total Ø",
                        score.getCoveredPercentage(),
                        score.getMissedPercentage(),
                        score.getImpact()));
            }
            details.append(formatItalicColumns(IMPACT,
                    renderImpact(configuration.getCoveredPercentageImpact()),
                    renderImpact(configuration.getMissedPercentageImpact()),
                    LEDGER));
        }
    }

    @Override
    protected void createSpecificSummary(final List<CoverageScore> scores, final StringBuilder summary) {
        for (CoverageScore score : scores) {
            summary.append("#");
            summary.append(getTitle(score));
            summary.append(String.format("%d%% %s, %d%% %s",
                    score.getCoveredPercentage(), getPlainText(coveredText),
                    score.getMissedPercentage(), getPlainText(missedText)));
            summary.append("\n");
        }
    }

    private String getPlainText(final String label) {
        return label.replace("%", "");
    }

    private int sum(final AggregatedScore score, final Function<CoverageScore, Integer> property) {
        return createScores(score).stream().map(property).reduce(Integer::sum).orElse(0);
    }
}
