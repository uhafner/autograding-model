package edu.hm.hafner.grading;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

import edu.hm.hafner.util.Generated;

/**
 * Computes the {@link Score} impact of code coverage results. These results are obtained by evaluating the covered or
 * uncovered percentage statistics.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public class CoverageScore extends Score {
    private static final long serialVersionUID = 1L;

    private final int coveredPercentage;

    /**
     * Creates a new {@link CoverageScore} instance.
     *
     * @param id
     *         the ID of the coverage
     * @param displayName
     *         display name of the coverage type (like line or branch coverage)
     * @param configuration
     *         the grading configuration
     * @param coveredPercentage
     *         the percentage (covered)
     */
    public CoverageScore(final String id, final String displayName, final CoverageConfiguration configuration,
            final int coveredPercentage) {
        super(id, displayName);

        this.coveredPercentage = coveredPercentage;

        setTotalImpact(computeImpact(configuration));
    }

    private int computeImpact(final CoverageConfiguration configuration) {
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

    @Override @Generated
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

    @Override @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), coveredPercentage);
    }

    @Override @Generated
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("coveredPercentage", coveredPercentage)
                .toString();
    }

    /**
     * A builder for {@link CoverageScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class CoverageScoreBuilder {
        private String id = "coverage";
        private String displayName = "Coverage";
        private CoverageConfiguration configuration = new CoverageConfiguration();

        private int coveredPercentage;

        /**
         * Sets the ID of the coverage score.
         *
         * @param id
         *         the ID
         *
         * @return this
         */
        public CoverageScoreBuilder withId(final String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the human readable name of the coverage score.
         *
         * @param displayName
         *         the name to show
         *
         * @return this
         */
        public CoverageScoreBuilder withDisplayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the grading configuration.
         *
         * @param configuration
         *         the grading configuration
         *
         * @return this
         */
        public CoverageScoreBuilder withConfiguration(final CoverageConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Sets the percentage (covered).
         *
         * @param coveredPercentage
         *         the percentage (covered)
         *
         * @return this
         */
        public CoverageScoreBuilder withCoveredPercentage(final int coveredPercentage) {
            this.coveredPercentage = coveredPercentage;
            return this;
        }

        /**
         * Builds the {@link CoverageScore} instance with the configured values.
         *
         * @return the new instance
         */
        public CoverageScore build() {
            return new CoverageScore(id, displayName, configuration, coveredPercentage);
        }
    }
}
