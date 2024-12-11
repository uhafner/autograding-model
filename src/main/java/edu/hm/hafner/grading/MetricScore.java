package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.Ensure;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Computes the {@link Score} impact of software metrics.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.DataClass")
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
                .map(v -> String.format("%s (%s)", v.asText(), metric.getAggregationType()))
                .orElse(N_A);
    }

    /**
     * A builder for {@link MetricScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class MetricScoreBuilder {
        @CheckForNull
        private String id;
        @CheckForNull
        private String name;
        private String icon = StringUtils.EMPTY;
        @CheckForNull
        private MetricConfiguration configuration;

        private final List<MetricScore> scores = new ArrayList<>();
        @CheckForNull
        private Metric metric;
        @CheckForNull
        private Node report;

        /**
         * Sets the human-readable name of the metric score.
         *
         * @param name
         *         the name to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public MetricScoreBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        private String getName() {
            return StringUtils.defaultIfBlank(name, getConfiguration().getName());
        }

        /**
         * Sets the icon of the metric score.
         *
         * @param icon
         *         the icon to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public MetricScoreBuilder withIcon(final String icon) {
            this.icon = icon;
            return this;
        }

        private String getIcon() {
            return StringUtils.defaultString(icon);
        }

        /**
         * Sets the grading configuration.
         *
         * @param configuration
         *         the grading configuration
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public MetricScoreBuilder withConfiguration(final MetricConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        private MetricConfiguration getConfiguration() {
            return Objects.requireNonNull(configuration);
        }

        /**
         * Sets the scores that should be aggregated by this score.
         *
         * @param scores
         *         the scores to aggregate
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public MetricScoreBuilder withScores(final List<MetricScore> scores) {
            Ensure.that(scores).isNotEmpty("You cannot add an empty list of scores.");
            this.scores.clear();
            this.scores.addAll(scores);

            return this;
        }

        /**
         * Sets the report with the issues that should be evaluated by this score.
         *
         * @param report
         *         the issues to evaluate
         * @param metric
         *        the metric to use
         * @return this
         */
        @CanIgnoreReturnValue
        public MetricScoreBuilder withReport(final Node report, final Metric metric) {
            this.report = report;
            this.metric = metric;

            return this;
        }

        /**
         * Builds the {@link MetricScore} instance with the configured values.
         *
         * @return the new instance
         */
        public MetricScore build() {
            Ensure.that(report != null ^ !scores.isEmpty()).isTrue(
                    "You must either specify a metric report or provide a list of sub-scores.");

            if (report == null || metric == null) {
                return new MetricScore(getName(), getIcon(), getConfiguration(), scores);
            }
            return new MetricScore(getName(), getIcon(), getConfiguration(),
                    Objects.requireNonNull(report), Objects.requireNonNull(metric));
        }
    }
}
