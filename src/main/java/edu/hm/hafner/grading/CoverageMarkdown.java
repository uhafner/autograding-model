package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

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
            final TruncatedStringBuilder details) {
        for (CoverageScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addNewline()
                    .addText(formatColumns("Name", coveredText, missedText, "Impact"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:"))
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(
                            subScore.getName(),
                            String.valueOf(subScore.getCoveredPercentage()),
                            String.valueOf(subScore.getMissedPercentage()),
                            String.valueOf(subScore.getImpact())))
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total Ø",
                                score.getCoveredPercentage(),
                                score.getMissedPercentage(),
                                score.getImpact()))
                        .addNewline();
            }

            var configuration = score.getConfiguration();
            details.addText(formatColumns(IMPACT))
                    .addText(formatItalicColumns(
                            renderImpact(configuration.getCoveredPercentageImpact()),
                            renderImpact(configuration.getMissedPercentageImpact())))
                    .addText(formatColumns(LEDGER))
                    .addNewline();
        }
    }
}
