package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class MetricMarkdown extends ScoreMarkdown<MetricScore, MetricConfiguration> {
    static final String TYPE = "Metrics Score";
    private static final String METRIC_ICON = "triangular_ruler";

    /**
     * Creates a new Markdown renderer for static analysis results.
     */
    public MetricMarkdown() {
        super(TYPE, METRIC_ICON);
    }

    @Override
    protected List<MetricScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getMetricScores();
    }

    @Override
    protected void createSpecificDetails(final AggregatedScore aggregation, final List<MetricScore> scores,
            final TruncatedStringBuilder details) {
        for (MetricScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(getPercentageImage(score))
                    .addNewline()
                    .addText(formatColumns("Name", "Value"))
                    .addTextIf(formatColumns("Impact"), score.hasMaxScore())
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:"))
                    .addTextIf(formatColumns(":-:"), score.hasMaxScore())
                    .addNewline();

            score.getSubScores().forEach(subScore -> details
                    .addText(formatColumns(subScore.getName(), subScore.createSummary()))
                    .addTextIf(formatColumns(String.valueOf(subScore.getImpact())), score.hasMaxScore())
                    .addNewline());

            details.addNewline();
        }
    }

    @Override
    protected String getToolIcon(final MetricScore score) {
        return switch (score.getMetric()) {
            case CYCLOMATIC_COMPLEXITY -> ":cyclone:";
            case NCSS -> ":memo:";
            case COGNITIVE_COMPLEXITY -> ":brain:";
            case NPATH_COMPLEXITY -> ":loop:";
            default -> getDefaultIcon(score);
        };
    }
}
