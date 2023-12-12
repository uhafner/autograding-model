package edu.hm.hafner.grading;

import java.util.List;

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
            details.append(getTitle(score));
            details.append(formatColumns("Name", coveredText, missedText, "Impact")).append("\n");
            details.append(formatColumns(":-:", ":-:", ":-:", ":-:")).append("\n");
            score.getSubScores().forEach(subScore -> details
                    .append(formatColumns(
                            subScore.getName(),
                            String.valueOf(subScore.getCoveredPercentage()),
                            String.valueOf(subScore.getMissedPercentage()),
                            String.valueOf(subScore.getImpact())))
                    .append("\n"));
            if (score.getSubScores().size() > 1) {
                details.append(formatBoldColumns("Total Ø",
                        score.getCoveredPercentage(),
                        score.getMissedPercentage(),
                        score.getImpact())).append("\n");
            }
            details.append(formatColumns(IMPACT));
            details.append(formatItalicColumns(
                    renderImpact(configuration.getCoveredPercentageImpact()),
                    renderImpact(configuration.getMissedPercentageImpact())));
            details.append(formatColumns(LEDGER));
            details.append("\n");
        }
    }

    @Override
    protected void createSpecificSummary(final List<CoverageScore> scores, final StringBuilder summary) {
        for (CoverageScore score : scores) {
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
}
