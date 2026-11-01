package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.MetricAggregation;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;
import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Computes the {@link Score} impact of software metrics.
 *
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public final class MetricScore extends Score<MetricScore, MetricConfiguration> {
    @Serial
    private static final long serialVersionUID = 15L;

    private static final String N_A = "<n/a>";
    private static final Metric AGGREGATION_METRIC = Metric.CONTAINER;

    private transient Node report; // do not persist the metrics tree
    private final Metric metric;

    private MetricScore(
            final String name,
            final String icon,
            final Scope scope,
            final MetricConfiguration configuration,
            final List<MetricScore> scores) {
        super(name, icon, scope, configuration, scores);

        this.report = new ContainerNode(name);

        var metrics = scores.stream()
                .map(MetricScore::getMetric)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (metrics.size() > 1) {
            this.metric = AGGREGATION_METRIC; // cannot aggregate different metrics
        } else {
            this.metric = metrics.iterator().next();
        }

        scores.stream().map(MetricScore::getReport).forEach(report::addChild);
    }

    private MetricScore(
            final String name,
            final String icon,
            final Scope scope,
            final MetricConfiguration configuration,
            final Node report,
            final Metric metric) {
        super(name, icon, scope, configuration, false); // TODO: shouldn't there be a delta as well?

        this.report = report;
        this.metric = metric;
    }

    /**
     * Restore an empty report after deserialization.
     *
     * @return this
     */
    @Serial
    @CanIgnoreReturnValue
    private Object readResolve() {
        report = new ModuleNode("empty");

        return this;
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * Returns the value of the metric.
     *
     * @return the value of the metric
     */
    public Value getMetricValue() {
        return getReport().getValue(metric).orElse(Value.nullObject(metric));
    }

    /**
     * Returns the value of the metric using the specified aggregation. The {@link MetricAggregation#OVERALL overall}
     * value combines all values of the report, while the {@link MetricAggregation#MAXIMUM maximum} and
     * {@link MetricAggregation#MINIMUM minimum} select the value of the element with the largest or smallest value
     * (e.g., the method with the largest cyclomatic complexity).
     *
     * @param aggregation the aggregation to use
     * @return the value of the metric, or an empty result if the report contains no such value
     */
    public Optional<Value> findMetricValue(final MetricAggregation aggregation) {
        if (metric == AGGREGATION_METRIC) {
            return Optional.empty(); // there is no aggregated value for multiple metrics
        }
        return getReport().getValue(metric, aggregation);
    }

    /**
     * Returns the value of the metric as an integer.
     *
     * @return the value of the metric
     */
    public String getMetricValueAsString() {
        return getReport().getValue(metric).map(v -> v.asText(Locale.ENGLISH)).orElse(N_A);
    }

    public String getMetricTagName() {
        return metric.toTagName();
    }

    @Override
    public int getImpact() {
        return 0;
    }

    @JsonIgnore
    public Node getReport() {
        return ObjectUtils.getIfNull(report, new ModuleNode("empty"));
    }

    @Override
    protected String createSummary() {
        if (metric == AGGREGATION_METRIC) {
            return N_A; // there is no aggregated value for multiple metrics
        }
        var defaultAggregation = metric.getDefaultAggregation();
        return getReport()
                .getValue(metric, defaultAggregation)
                .map(value -> formatValue(value, defaultAggregation) + formatExtremeValues(defaultAggregation))
                .orElse(N_A);
    }

    private String formatExtremeValues(final MetricAggregation defaultAggregation) {
        return Stream.of(MetricAggregation.MINIMUM, MetricAggregation.MAXIMUM)
                .filter(aggregation -> aggregation != defaultAggregation)
                .map(aggregation -> findMetricValue(aggregation)
                        .map(value -> ", " + formatValue(value, aggregation))
                        .orElse(StringUtils.EMPTY))
                .collect(Collectors.joining());
    }

    private String formatValue(final Value value, final MetricAggregation aggregation) {
        return "%s (%s)".formatted(value.asText(Locale.ENGLISH), aggregation);
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        var that = (MetricScore) o;
        return metric == that.metric;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), metric);
    }

    /** A builder for {@link MetricScore} instances. */
    static class MetricScoreBuilder extends ScoreBuilder<MetricScore, MetricConfiguration> {
        MetricScoreBuilder() {
            this(NO_DELTA_REPORTS);
        }

        MetricScoreBuilder(final String deltaReportsPath) {
            super(deltaReportsPath);
        }

        @Override
        MetricScore aggregate(final List<MetricScore> scores) {
            return new MetricScore(getTopLevelName(), getIcon(), getScope(), getConfiguration(), scores);
        }

        @Override
        MetricScore build() {
            return new MetricScore(getName(), getIcon(), getScope(), getConfiguration(), getNode(), getMetric());
        }

        @Override
        void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readNode(factory, tool, log);
        }

        @Override
        String getType() {
            return "metric";
        }

        @Override
        String getDefaultTopLevelName() {
            return "Metrics Results";
        }
    }
}
