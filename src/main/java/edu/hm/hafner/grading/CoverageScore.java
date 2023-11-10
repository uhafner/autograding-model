package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

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
public class CoverageScore extends Score<CoverageScore, CoverageConfiguration> {
    @Serial
    private static final long serialVersionUID = 3L;

    private final int coveredPercentage;
    private final Metric metric;
    @CheckForNull
    private final transient Node rootNode;

    private CoverageScore(final String id, final String name, final CoverageConfiguration configuration,
            final List<CoverageScore> scores) {
        super(id, name, configuration, scores.toArray(new CoverageScore[0]));

        this.coveredPercentage = scores.stream()
                .reduce(0, (sum, score) -> sum + score.getCoveredPercentage(), Integer::sum)
                / scores.size();
        this.metric = scores.stream().map(CoverageScore::getMetric).filter(Objects::nonNull).findFirst().orElseThrow(
                () -> new IllegalArgumentException("No metric found in scores."));
        this.rootNode = new ModuleNode("empty");
    }

    private CoverageScore(final String id, final String name, final CoverageConfiguration configuration,
            final Node rootNode, final Metric metric) {
        super(id, name, configuration);

        this.rootNode = rootNode;
        this.metric = metric;

        var value = rootNode.getValue(metric);
        if (value.isPresent() && value.get() instanceof Coverage) {
            this.coveredPercentage = ((Coverage) value.get()).getCoveredPercentage().toInt();
        }
        else {
            throw new IllegalArgumentException(String.format(
                    "The coverage node for '%s' does not contain a value for metric %s.", name, metric.toTagName()));
        }
    }

    public Metric getMetric() {
        return metric;
    }

    @CheckForNull @JsonIgnore
    public Node getRootNode() {
        return ObjectUtils.defaultIfNull(rootNode, new ModuleNode("empty"));
    }

    @Override
    public int getImpact() {
        CoverageConfiguration configuration = getConfiguration();

        int change = 0;

        change = change + configuration.getMissedPercentageImpact() * getMissedPercentage();
        change = change + configuration.getCoveredPercentageImpact() * getCoveredPercentage();

        return change;
    }

    public final int getCoveredPercentage() {
        return coveredPercentage;
    }

    public final int getMissedPercentage() {
        return 100 - coveredPercentage;
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
        CoverageScore that = (CoverageScore) o;
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
        private String id;
        private String name;
        private CoverageConfiguration configuration;

        private final List<CoverageScore> scores = new ArrayList<>();
        @CheckForNull
        private Metric metric;
        @CheckForNull
        private Node rootNode;

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
            return StringUtils.defaultIfBlank(id, configuration.getId());
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
            return StringUtils.defaultIfBlank(name, configuration.getName());
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
            this.rootNode = rootNode;
            this.metric = metric;
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
            Ensure.that((rootNode != null && metric != null) ^ !scores.isEmpty()).isTrue(
                    "You must either specify a coverage report or provide a list of sub-scores.");

            if (scores.isEmpty()) {
                return new CoverageScore(getId(), getName(), configuration, rootNode, metric);
            }
            else {
                return new CoverageScore(getId(), getName(), configuration, scores);
            }
        }
    }
}
