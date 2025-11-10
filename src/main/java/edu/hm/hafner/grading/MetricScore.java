package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.hm.hafner.coverage.*;
import edu.hm.hafner.util.FilteredLog;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serial;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Computes the {@link Score} impact of software metrics.
 *
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public final class MetricScore extends Score<MetricScore, MetricConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final String N_A = "<n/a>";
    private static final Metric AGGREGATION_METRIC = Metric.CONTAINER;

    private transient Node report; // do not persist the metrics tree
    private transient Node deltaReport;
    private final Metric metric;

    private MetricScore(final String name, final String icon, final Scope scope, final MetricConfiguration configuration,
            final List<MetricScore> scores) {
        super(name, icon, scope, configuration, scores.toArray(new MetricScore[0]));

        this.report = new ContainerNode(name);
        this.deltaReport = new ContainerNode(name +  "_delta");

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

    private MetricScore(final String name, final String icon, final Scope scope, final MetricConfiguration configuration,
            final Node report, final Node deltaReport, final Metric metric) {
        super(name, icon, scope, configuration);

        this.report = report;
        this.deltaReport = deltaReport;
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
        deltaReport = new ModuleNode("empty_delta");

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
        return ObjectUtils.getIfNull(report, new ModuleNode("empty"));
    }

    @JsonIgnore
    public Node getDeltaReport() {
        return ObjectUtils.getIfNull(deltaReport, new ModuleNode("empty_delta"));
    }

    @Override
    public boolean hasDelta() {
        return !getDeltaReport().isEmpty();
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
            return new MetricScore(getTopLevelName(), getIcon(), getScope(), getConfiguration(), scores);
        }

        @Override
        public MetricScore build() {
            return new MetricScore(getName(), getIcon(), getScope(), getConfiguration(), getNode(), getDeltaNode(), getMetric());
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
