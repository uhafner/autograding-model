package edu.hm.hafner.grading;

import edu.hm.hafner.coverage.MetricAggregation;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.grading.TruncatedString.TruncatedStringBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public class MetricMarkdown extends ScoreMarkdown<MetricScore, MetricConfiguration> {
    static final String TYPE = "Metrics Score";
    private static final String METRIC_ICON = emoji("triangular_ruler");
    private static final double MEDIAN = 50.0; // percentile in the range (0, 100]

    /** Creates a new Markdown renderer for static analysis results. */
    public MetricMarkdown() {
        super(TYPE, METRIC_ICON);
    }

    @Override
    List<MetricScore> createScores(final AggregatedScore aggregation) {
        return aggregation.getMetricScores();
    }

    @Override
    String createSpecificDetails(final List<MetricScore> scores) {
        var details = new TruncatedStringBuilder();
        for (MetricScore score : scores) {
            details.addText(getTitle(score, 2))
                    .addParagraph()
                    .addText(formatColumns("Icon", "Name", "Scope", "Total", "Min", "Max", "Mean", "Median"))
                    .addNewline()
                    .addText(formatColumns(":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:", ":-:"))
                    .addNewline();

            score.getSubScores().stream().map(this::createMetricRow).forEach(details::addText);

            details.addNewline();
        }
        return details.build().buildByChars(MARKDOWN_MAX_SIZE);
    }

    private String createMetricRow(final MetricScore score) {
        if (score.getReport().getValue(score.getMetric()).isEmpty()) {
            return formatColumns(
                            getIcon(score), score.getName(), score.getScope().getDisplayName(), N_A, N_A, N_A, N_A, N_A)
                    + LINE_BREAK;
        }
        return createRow(score) + LINE_BREAK;
    }

    private String createRow(final MetricScore score) {
        var stats = new DescriptiveStatistics();
        var metric = score.getMetric();
        metric.getTargetNodes(score.getReport()).stream()
                .map(node -> node.getValue(metric))
                .flatMap(Optional::stream)
                .map(Value::asDouble)
                .forEach(stats::addValue);
        return formatColumns(
                getIcon(score),
                score.getName(),
                score.getScope().getDisplayName(),
                score.getMetricValueAsString(),
                formatValue(score, MetricAggregation.MINIMUM),
                formatValue(score, MetricAggregation.MAXIMUM),
                formatStatistics(stats, () -> metric.formatMean(Locale.ENGLISH, stats.getMean())),
                formatStatistics(stats, () -> metric.format(Locale.ENGLISH, stats.getPercentile(MEDIAN))));
    }

    private String formatValue(final MetricScore score, final MetricAggregation aggregation) {
        return score.findMetricValue(aggregation)
                .map(value -> value.asText(Locale.ENGLISH))
                .orElse(N_A);
    }

    private String formatStatistics(final DescriptiveStatistics stats, final Supplier<String> formatter) {
        if (stats.getN() == 0) {
            return N_A;
        }
        return formatter.get();
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
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
