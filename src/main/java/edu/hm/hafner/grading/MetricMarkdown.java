package edu.hm.hafner.grading;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class MetricMarkdown extends ScoreMarkdown<MetricScore, MetricConfiguration> {
    static final String TYPE = "Metrics Score";
    private static final String METRIC_ICON = emoji("triangular_ruler");

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
    protected String createSpecificDetails(final List<MetricScore> scores) {
        var details = new TruncatedStringBuilder();
        for (MetricScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(getPercentageImage(score))
                    .addNewline()
                    .addText(formatColumns("Icon", "Name", "Total", "Min", "Max", "Mean", "Median"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addNewline();

            score.getSubScores().stream().map(this::createMetricRow).forEach(details::addText);

            details.addNewline();
        }
        return details.build().buildByChars(MARKDOWN_MAX_SIZE);
    }

    private String createMetricRow(final MetricScore score) {
        if (score.getReport().getValue(score.getMetric()).isEmpty()) {
            return formatColumns(getIcon(score), score.getName(), N_A, N_A, N_A, N_A, N_A);
        }
        return createRow(score) + "\n";
    }

    private String createRow(final MetricScore score) {
        var stats = new DescriptiveStatistics();
        var metric = score.getMetric();
        metric.getTargetNodes(score.getReport()).stream()
                .map(node -> node.getValue(metric))
                .flatMap(Optional::stream)
                .map(Value::asDouble)
                .forEach(stats::addValue);
        return formatColumns(getIcon(score), score.getName(), score.getMetricValueAsString(),
                metric.format(Locale.ENGLISH, stats.getMin()),
                metric.format(Locale.ENGLISH, stats.getMax()),
                metric.formatMean(Locale.ENGLISH, stats.getMean()),
                metric.format(Locale.ENGLISH, stats.getPercentile(0.5)));
    }

    @Override @SuppressWarnings("PMD.CyclomaticComplexity")
    protected String getToolIcon(final MetricScore score) {
        return switch (score.getMetric()) {
            case CYCLOMATIC_COMPLEXITY -> ":cyclone:";
            case COGNITIVE_COMPLEXITY -> ":thought_balloon:";
            case LOC -> ":straight_ruler:";
            case NCSS -> ":memo:";
            case ACCESS_TO_FOREIGN_DATA -> ":telescope:";
            case COHESION -> ":link:";
            case FAN_OUT -> ":outbox_tray:";
            case NUMBER_OF_ACCESSORS -> ":calling:";
            case WEIGHT_OF_CLASS -> ":balance_scale:";
            case NPATH_COMPLEXITY -> ":loop:";
            default -> getDefaultIcon(score);
        };
    }
}
