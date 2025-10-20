package edu.hm.hafner.grading;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.function.Predicate;

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

    protected boolean containsMutationMetrics(final CoverageScore score) {
        return score.getSubScores().stream()
                .anyMatch(subScore -> subScore.getMetric() == Metric.MUTATION
                        || subScore.getMetric() == Metric.TEST_STRENGTH);
    }

    @Override
    protected final List<CoverageScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getCoverageScores().stream()
                .filter(filterScores())
                .toList();
    }

    protected abstract Predicate<CoverageScore> filterScores();

    @Override
    protected String createSpecificDetails(final List<CoverageScore> scores) {
        var details = new TruncatedStringBuilder();
        for (CoverageScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(getImageForScoreOrCoverage(score))
                    .addNewline()
                    .addText(formatColumns("Icon", "Name", coveredText, missedText))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> addSubScoreRows(details, score, subScore));

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns(":heavy_plus_sign:", "Total Ø",
                                score.getCoveredPercentage(),
                                score.getMissedPercentage()))
                        .addTextIf(formatBoldColumns(score.getImpact()), score.hasMaxScore())
                        .addNewline();
            }

            if (score.hasMaxScore()) {
                var configuration = score.getConfiguration();
                details.addText(formatColumns(IMPACT, EMPTY))
                        .addText(formatItalicColumns(
                                renderImpact(configuration.getCoveredPercentageImpact()),
                                renderImpact(configuration.getMissedPercentageImpact())))
                        .addText(formatColumns(LEDGER))
                        .addNewline();
            }

            details.addNewline();
        }
        return details.build().buildByChars(MARKDOWN_MAX_SIZE);
    }

    private String getImageForScoreOrCoverage(final CoverageScore score) {
        if (score.hasMaxScore()) { // show the score percentage
            return getPercentageImage(score.getName(), score.getPercentage());
        }
        return getPercentageImage(score.getName(), score.getCoveredPercentage());
    }

    /**
     * Adds rows for a sub-score, including both PROJECT and MODIFIED_LINES baselines.
     *
     * @param details the string builder
     * @param score the parent score
     * @param subScore the sub-score to add
     */
    private void addSubScoreRows(final TruncatedStringBuilder details, final CoverageScore score,
                                 final CoverageScore subScore) {
        // Project baseline row
        details
                .addText(formatColumns(getIcon(subScore), subScore.getName(),
                        String.valueOf(subScore.getCoveredPercentage()),
                        String.valueOf(subScore.getMissedPercentage())))
                .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                .addNewline();

        // Modified lines baseline row (if available)
        int modifiedPercentage = subScore.computeModifiedLinesPercentage();
        if (modifiedPercentage >= 0) {
            int modifiedMissed = 100 - modifiedPercentage;
            details
                    .addText(formatColumns("", "_Modified Lines_",
                            String.valueOf(modifiedPercentage),
                            String.valueOf(modifiedMissed)))
                    .addTextIf(formatColumns(""), score.hasMaxScore())
                    .addNewline();
        }
    }


    @Override
    protected String getToolIcon(final CoverageScore score) {
        return switch (score.getMetric()) {
            case BRANCH -> emoji("curly_loop");
            case LINE -> emoji("wavy_dash");
            case CYCLOMATIC_COMPLEXITY -> emoji("part_alternation_mark");
            case LOC -> emoji("pencil2");
            case TEST_STRENGTH ->  emoji("muscle");
            default -> getDefaultIcon(score);
        };
    }
}
