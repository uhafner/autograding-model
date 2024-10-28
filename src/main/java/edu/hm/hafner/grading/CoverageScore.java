package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Computes the {@link Score} impact of code coverage results. These results are obtained by evaluating the covered or
 * uncovered percentage statistics.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public final class CoverageScore extends Score<CoverageScore, CoverageConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final Metric AGGREGATION_METRIC = Metric.CONTAINER;

    private final int coveredPercentage;
    private final Metric metric;
    private final String icon;
    private final int missedItems;
    private transient Node report; // do not persist the coverage tree

    private CoverageScore(final String id, final String name, final CoverageConfiguration configuration,
            final List<CoverageScore> scores) {
        super(id, name, configuration, scores.toArray(new CoverageScore[0]));

        this.coveredPercentage = scores.stream()
                .reduce(0, (sum, score) -> sum + score.getCoveredPercentage(), Integer::sum)
                / scores.size();
        this.missedItems = scores.stream()
                .reduce(0, (sum, score) -> sum + score.getMissedItems(), Integer::sum);
        var metrics = scores.stream()
                .map(CoverageScore::getMetric)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (metrics.size() > 1) {
            this.metric = AGGREGATION_METRIC; // cannot aggregate different metrics
        }
        else {
            this.metric = metrics.iterator().next();
        }
        this.icon = selectIcon();

        this.report = new ContainerNode(name);
        scores.stream().map(CoverageScore::getReport).forEach(report::addChild);
    }

    private CoverageScore(final String id, final String name, final CoverageConfiguration configuration,
            final Node report, final Metric metric) {
        super(id, name, configuration);

        this.report = report;
        this.metric = metric;
        this.icon = selectIcon();

        var value = report.getValue(metric);
        if (value.isPresent() && value.get() instanceof Coverage) {
            this.coveredPercentage = ((Coverage) value.get()).getCoveredPercentage().toInt();
            this.missedItems = ((Coverage) value.get()).getMissed();
        }
        else {
            this.coveredPercentage = 0; // If there is no coverage, then there is no code yet
            this.missedItems = 0;
        }
    }

    public int getMissedItems() {
        return missedItems;
    }

    public String getIcon() {
        return icon;
    }

    private String selectIcon() {
        switch (metric) {
            case BRANCH -> {
                return "curly_loop";
            }
            case LINE -> {
                return "wavy_dash";
            }
            case CYCLOMATIC_COMPLEXITY -> {
                return "part_alternation_mark";
            }
            case LOC -> {
                return "pencil2";
            }
            case TEST_STRENGTH ->  {
                return "muscle";
            }
            default -> {
                return "footprints";
            }
        }
    }

    /**
     * Restore an empty report after de-serialization.
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

    public String getMetricTagName() {
        return metric.toTagName();
    }

    @JsonIgnore
    public Node getReport() {
        return report;
    }

    @Override
    public int getImpact() {
        var configuration = getConfiguration();

        int change = 0;

        change = change + configuration.getMissedPercentageImpact() * getMissedPercentage();
        change = change + configuration.getCoveredPercentageImpact() * getCoveredPercentage();

        return change;
    }

    public int getCoveredPercentage() {
        return coveredPercentage;
    }

    public int getMissedPercentage() {
        return 100 - coveredPercentage;
    }

    @Override
    protected String createSummary() {
        return format("%d%% (%d %s)", getCoveredPercentage(), getMissedItems(), getItemName());
    }

    private String getItemName() {
        switch (metric) {
            case MUTATION -> {
                return "survived mutations";
            }
            case TEST_STRENGTH -> {
                return "survived mutations in tested code";
            }
            case BRANCH -> {
                return "missed branches";
            }
            case LINE -> {
                return "missed lines";
            }
            case CYCLOMATIC_COMPLEXITY -> {
                return "complexity";
            }
            case LOC -> {
                return "lines of code";
            }
            case CONTAINER -> {
                return "missed items";
            }
            default -> {
                return "items";
            }
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        var that = (CoverageScore) o;
        return coveredPercentage == that.coveredPercentage;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), coveredPercentage);
    }

    /**
     * A builder for {@link CoverageScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class CoverageScoreBuilder {
        @CheckForNull
        private String id;
        @CheckForNull
        private String name;
        @CheckForNull
        private CoverageConfiguration configuration;

        private final List<CoverageScore> scores = new ArrayList<>();
        @CheckForNull
        private Metric metric;
        @CheckForNull
        private Node report;

        /**
         * Sets the ID of the coverage score.
         *
         * @param id
         *         the ID
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageScoreBuilder withId(final String id) {
            this.id = id;
            return this;
        }

        private String getId() {
            return StringUtils.defaultIfBlank(id, getConfiguration().getId());
        }

        /**
         * Sets the human-readable name of the coverage score.
         *
         * @param name
         *         the name to show
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageScoreBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        private String getName() {
            return StringUtils.defaultIfBlank(name, getConfiguration().getName());
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
        public CoverageScoreBuilder withConfiguration(final CoverageConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        private CoverageConfiguration getConfiguration() {
            return Objects.requireNonNull(configuration);
        }

        /**
         * Sets the coverage report for this score.
         *
         * @param rootNode
         *         the root of the coverage tree
         * @param metric
         *         the metric to use
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageScoreBuilder withReport(final Node rootNode, final Metric metric) {
            this.report = rootNode;
            this.metric = metric;

            Ensure.that(metric.isCoverage()).isTrue("The metric must be a coverage metric, but is %s", metric);

            return this;
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
        public CoverageScoreBuilder withScores(final List<CoverageScore> scores) {
            Ensure.that(scores).isNotEmpty("You cannot add an empty list of scores.");
            this.scores.clear();
            this.scores.addAll(scores);
            return this;
        }

        /**
         * Builds the {@link CoverageScore} instance with the configured values.
         *
         * @return the new instance
         */
        public CoverageScore build() {
            Ensure.that((report != null && metric != null) ^ !scores.isEmpty()).isTrue(
                    "You must either specify a coverage report or provide a list of sub-scores.");

            if (scores.isEmpty() && report != null && metric != null) {
                return new CoverageScore(getId(), getName(), getConfiguration(),
                        Objects.requireNonNull(report),
                        Objects.requireNonNull(metric));
            }
            else {
                return new CoverageScore(getId(), getName(), getConfiguration(), scores);
            }
        }
    }
}
