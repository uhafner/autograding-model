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
                    .addParagraph()
                    .addText(getImageForScoreOrCoverage(score))
                    .addNewline()
                    .addText(formatColumns("Name", coveredText, missedText))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(
                            subScore.getName(),
                            String.valueOf(subScore.getCoveredPercentage()),
                            String.valueOf(subScore.getMissedPercentage())))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns("Total Ø",
                                score.getCoveredPercentage(),
                                score.getMissedPercentage()))
                        .addTextIf(formatBoldColumns(score.getImpact()), score.hasMaxScore())
                        .addNewline();
            }

            if (score.hasMaxScore()) {
                var configuration = score.getConfiguration();
                details.addText(formatColumns(IMPACT))
                        .addText(formatItalicColumns(
                                renderImpact(configuration.getCoveredPercentageImpact()),
                                renderImpact(configuration.getMissedPercentageImpact())))
                        .addText(formatColumns(LEDGER))
                        .addNewline();
            }

            details.addNewline();
        }
    }

    private String getImageForScoreOrCoverage(final CoverageScore score) {
        if (score.hasMaxScore()) { // show the score percentage
            return getPercentageImage(score.getDisplayName(), score.getPercentage());
        }
        return getPercentageImage(score.getDisplayName(), score.getCoveredPercentage());
    }

    @Override
    protected List<String> createSummary(final CoverageScore score) {
        return score.getSubScores().stream()
                .map(s -> SPACE + SPACE + getTitle(s, 0) + ": " + s.createSummary()).toList();
    }
}
