package edu.hm.hafner.grading;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;

import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Computes the {@link Score} impact of software metrics.
 *
 * @author Ullrich Hafner
 */
public final class MetricScore extends Score<MetricScore, MetricConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final String N_A = "<n/a>";
    private static final Metric AGGREGATION_METRIC = Metric.CONTAINER;

    private transient Node report; // do not persist the metrics tree
    private final Metric metric;

    private MetricScore(final String name, final String icon, final MetricConfiguration configuration,
            final List<MetricScore> scores) {
        super(name, icon, configuration, scores.toArray(new MetricScore[0]));

        this.report = new ContainerNode(name);

        var metrics = scores.stream()
                .map(MetricScore::getMetric)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (metrics.size() > 1) {
            this.metric = AGGREGATION_METRIC; // cannot aggregate different metrics
        }
        else {
            this.metric = metrics.iterator().next();
        }

        scores.stream().map(MetricScore::getReport).forEach(report::addChild);
    }

    private MetricScore(final String name, final String icon, final MetricConfiguration configuration,
            final Node report, final Metric metric) {
        super(name, icon, configuration);

        this.report = report;
        this.metric = metric;
    }

    /**
     * Restore an empty report after deserialization.
     *
     * @return this
     */
    @Serial @CanIgnoreReturnValue
    private Object readResolve() {
        report = new ModuleNode("empty");

        return this;
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * Returns the value of the metric as an integer.
     *
     * @return the value of the metric
     */
    public int getMetricValue() {
        return getReport().getValue(metric)
                .map(Value::asInteger)
                .orElse(0);
    }

    /**
     * Returns the value of the metric as an integer.
     *
     * @return the value of the metric
     */
    public String getMetricValueAsString() {
        return getReport().getValue(metric)
                .map(v -> v.asText(Locale.ENGLISH))
                .orElse(N_A);
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
        return ObjectUtils.defaultIfNull(report, new ModuleNode("empty"));
    }

    @Override
    protected String createSummary() {
        if (metric == AGGREGATION_METRIC) {
            return N_A; // there is no aggregated value for multiple metrics
        }
        return getReport().getValue(metric)
                .map(v -> "%s (%s)".formatted(v.asText(Locale.ENGLISH), metric.getAggregationType()))
                .orElse(N_A);
    }

    /**
     * A builder for {@link MetricScore} instances.
     */
    static class MetricScoreBuilder extends ScoreBuilder<MetricScore, MetricConfiguration> {
        @Override
        public MetricScore aggregate(final List<MetricScore> scores) {
            return new MetricScore(getTopLevelName(), getIcon(), getConfiguration(), scores);
        }

        @Override
        public MetricScore build() {
            return new MetricScore(getName(), getIcon(), getConfiguration(), getNode(), getMetric());
        }

        @Override
        public void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readNode(factory, tool, log);
        }

        @Override
        public String getType() {
            return "metric";
        }

        @Override
        String getDefaultTopLevelName() {
            return "Metrics Results";
        }
    }
}
