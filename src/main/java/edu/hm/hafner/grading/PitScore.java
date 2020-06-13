package edu.hm.hafner.grading;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

import edu.hm.hafner.util.Generated;

/**
 * Computes the {@link Score} impact of PIT mutation testing results. These results are obtained by evaluating the
 * detected or undetected mutation statistics (relative or absolute).
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public class PitScore extends Score {
    private static final long serialVersionUID = 1L;

    static final String ID = "pit";

    private final int mutationsSize;
    private final int undetectedSize;
    private final int undetectedPercentage;

    /**
     * Creates a new {@link PitScore} instance.
     *
     * @param displayName
     *         the human readable name of PIT
     * @param configuration
     *         the grading configuration
     * @param totalMutations
     *         total number of mutations
     * @param undetectedMutations
     *         number of undetected mutations
     */
    PitScore(final String displayName, final PitConfiguration configuration, final int totalMutations,
            final int undetectedMutations) {
        super(ID, displayName);

        mutationsSize = totalMutations;
        undetectedSize = undetectedMutations;
        undetectedPercentage = undetectedSize * 100 / mutationsSize;

        setTotalImpact(computeImpact(configuration));
    }

    private int computeImpact(final PitConfiguration configs) {
        int change = 0;

        change = change + configs.getUndetectedImpact() * getUndetectedSize();
        change = change + configs.getUndetectedPercentageImpact() * getUndetectedPercentage();
        change = change + configs.getDetectedImpact() * getDetectedSize();
        change = change + configs.getDetectedPercentageImpact() * getDetectedPercentage();

        return change;
    }

    public final int getMutationsSize() {
        return mutationsSize;
    }

    public final int getUndetectedSize() {
        return undetectedSize;
    }

    public final int getDetectedSize() {
        return mutationsSize - undetectedSize;
    }

    public final int getUndetectedPercentage() {
        return undetectedPercentage;
    }

    public final int getDetectedPercentage() {
        return 100 - undetectedPercentage;
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
        PitScore pitScore = (PitScore) o;
        return mutationsSize == pitScore.mutationsSize
                && undetectedSize == pitScore.undetectedSize
                && undetectedPercentage == pitScore.undetectedPercentage;
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), mutationsSize, undetectedSize, undetectedPercentage);
    }

    @Override @Generated
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("mutationsSize", mutationsSize)
                .append("undetectedSize", undetectedSize)
                .append("undetectedPercentage", undetectedPercentage)
                .toString();
    }

    /**
     * A builder for {@link PitScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class PitScoreBuilder {
        private String displayName = "PIT Mutation Coverage";

        private PitConfiguration configuration;
        private int totalMutations;
        private int undetectedMutations;

        /**
         * Sets the human readable name of the coverage score.
         *
         * @param displayName
         *         the name to show
         *
         * @return this
         */
        public PitScoreBuilder withDisplayName(final String displayName) {
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
        public PitScoreBuilder withConfiguration(final PitConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Returns the total number of mutations.
         *
         * @param totalMutations
         *         total number of mutations
         *
         * @return this
         */
        public PitScoreBuilder withTotalMutations(final int totalMutations) {
            this.totalMutations = totalMutations;
            return this;
        }

        /**
         * Returns the total number of undetected mutations.
         *
         * @param undetectedMutations
         *         total number of undetected mutations
         *
         * @return this
         */
        public PitScoreBuilder withUndetectedMutations(final int undetectedMutations) {
            this.undetectedMutations = undetectedMutations;
            return this;
        }

        /**
         * Builds the {@link PitScore} instance with the configured values.
         *
         * @return the new instance
         */
        public PitScore build() {
            return new PitScore(displayName, configuration, totalMutations, undetectedMutations);
        }
    }
}
