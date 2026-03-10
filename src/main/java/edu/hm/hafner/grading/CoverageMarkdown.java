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
    private static final double SUNNY_PERCENTAGE = 90.0;
    private static final double SMALL_CLOUD_PERCENTAGE = 80.0;
    private static final double PARTLY_CLOUDED_PERCENTAGE = 70.0;

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
    String createScoreSummary(final CoverageScore score) {
        var percentage = format("%.2f%%", score.getCoveredPercentage());
        String items;
        if (score.isPerfect()) {
            items = MDASH + " perfect :tada:";
        }
        else {
            items = format(MDASH + " %s %s", score.getMissedItems(), CoverageScore.getItemName(score.getMetric()));
        }

        if (score.hasDelta()) {
            return format("%s %s %s", percentage, delta(score.getCoveredPercentageDelta(), true), items);
        }
        return format("%s %s", percentage, items);
    }

    @Override
    protected String createSpecificDetails(final List<CoverageScore> scores) {
        var details = new TruncatedStringBuilder();
        for (CoverageScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(formatColumns("Icon", "Name", "Scope", coveredText))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addText(formatColumns("Status"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addText(formatColumns(":-:"))
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(getIcon(subScore), subScore.getName(), subScore.getScope().getDisplayName(),
                            deltaCell(subScore.hasDelta(), subScore.getCoveredPercentage(), subScore.getCoveredPercentageDelta(),
                                    true)))
                    .addTextIf(formatColumns(subScore.getImpact()), score.hasMaxScore())
                    .addText(formatColumns(createStatus(subScore)))
                    .addNewline());

            if (score.getSubScores().size() > 1) {
                details.addText(formatBoldColumns(":heavy_plus_sign:", "Total", EMPTY,
                                deltaCell(score.hasDelta(), score.getCoveredPercentage(), score.getCoveredPercentageDelta(),
                                        true)))
                        .addTextIf(formatBoldColumns(score.getImpact()), score.hasMaxScore())
                        .addText(formatColumns(createStatus(score)))
                        .addNewline();
            }

            details.addNewline();
        }
        return details.build().buildByChars(MARKDOWN_MAX_SIZE);
    }

    private String createStatus(final CoverageScore score) {
        if (score.getMissedItems() == 0) {
            return emoji("tada");
        }
        if (score.getCoveredPercentage() >= SUNNY_PERCENTAGE) {
            return emoji("sunny");
        }
        if (score.getCoveredPercentage() >= SMALL_CLOUD_PERCENTAGE) {
            return emoji("sun_behind_small_cloud");
        }
        if (score.getCoveredPercentage() >= PARTLY_CLOUDED_PERCENTAGE) {
            return emoji("partly_sunny");
        }
        return emoji("cloud");
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
