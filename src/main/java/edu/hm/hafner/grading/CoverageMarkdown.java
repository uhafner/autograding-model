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
 * @author Jannik Ohme
 */
abstract class CoverageMarkdown extends ScoreMarkdown<CoverageScore, CoverageConfiguration> {
    private final String coveredText;

    CoverageMarkdown(final String type, final String icon, final String coveredText) {
        super(type, icon);

        this.coveredText = coveredText;
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
                    .addText(formatColumns("Icon", "Name", "Scope", coveredText))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(getIcon(subScore), subScore.getName(), subScore.getScope().getDisplayName(),
                            String.valueOf(subScore.getCoveredPercentage())))
                    .addTextIf(formatColumns(subScore.getImpact()), score.hasMaxScore())
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns(":heavy_plus_sign:", "Total Ø", EMPTY,
                                score.getCoveredPercentage()))
                        .addTextIf(formatBoldColumns(score.getImpact()), score.hasMaxScore())
                        .addNewline();
            }

            details.addNewline();
        }
        return details.build().buildByChars(MARKDOWN_MAX_SIZE);
    }

    @Override
    protected String getToolIcon(final CoverageScore score) {
        return switch (score.getMetric()) {
            case BRANCH -> emoji("curly_loop");
            case LINE -> emoji("wavy_dash");
            case CYCLOMATIC_COMPLEXITY -> emoji("part_alternation_mark");
            case LOC -> emoji("pencil2");
            case TEST_STRENGTH -> emoji("muscle");
            default -> getDefaultIcon(score);
        };
    }
}
