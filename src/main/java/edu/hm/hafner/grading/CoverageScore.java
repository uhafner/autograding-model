package edu.hm.hafner.grading;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final int missedItems;
    private transient Node report; // do not persist the coverage tree

    private CoverageScore(final String name, final String icon, final CoverageConfiguration configuration,
            final List<CoverageScore> scores) {
        super(name, icon, configuration, scores.toArray(new CoverageScore[0]));

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

        this.report = new ContainerNode(name);
        scores.stream().map(CoverageScore::getReport).forEach(report::addChild);
    }

    private CoverageScore(final String name, final String icon, final CoverageConfiguration configuration,
            final Node report, final Metric metric) {
        super(name, icon, configuration);

        this.report = report;
        this.metric = metric;

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

        change = change + scale(configuration.getMissedPercentageImpact(), getMissedPercentage());
        change = change + scale(configuration.getCoveredPercentageImpact(), getCoveredPercentage());

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
        return switch (metric) {
            case MUTATION -> "survived mutations";
            case TEST_STRENGTH -> "survived mutations in tested code";
            case BRANCH -> "missed branches";
            case LINE -> "missed lines";
            case CYCLOMATIC_COMPLEXITY -> "complexity";
            case LOC -> "lines of code";
            case CONTAINER -> "missed items";
            default -> "items";
        };
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
    static class CoverageScoreBuilder extends ScoreBuilder<CoverageScore, CoverageConfiguration> {
        @Override
        public CoverageScore aggregate(final List<CoverageScore> scores) {
            return new CoverageScore(getTopLevelName(), getIcon(), getConfiguration(), scores);
        }

        @Override
        public CoverageScore build() {
            return new CoverageScore(getName(), getIcon(), getConfiguration(), getNode(), getMetric());
        }

        @Override
        public void read(final ToolParser factory, final ToolConfiguration tool, final FilteredLog log) {
            readNode(factory, tool, log);
        }

        @Override
        public String getType() {
            return "coverage";
        }

        @Override
        String getDefaultTopLevelName() {
            return "Coverage Results";
        }
    }
}
