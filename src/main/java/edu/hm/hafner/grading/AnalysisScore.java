package edu.hm.hafner.grading;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

import edu.hm.hafner.util.Generated;

/**
 * Computes the {@link Score} impact of static analysis results. These results are obtained by summing up the number of
 * static analysis warnings.
 *
 * @author Eva-Maria Zeintl
 */
@SuppressWarnings("PMD.DataClass")
public class AnalysisScore extends Score {
    private static final long serialVersionUID = 1L;

    private final int errorsSize;
    private final int highSeveritySize;
    private final int normalSeveritySize;
    private final int lowSeveritySize;

    /**
     * Creates a new {@link AnalysisScore} instance.
     *
     * @param id
     *         the ID of the analysis tool
     * @param displayName
     *         the human readable name of the analysis tool
     * @param configuration
     *         the grading configuration
     * @param totalErrorsSize
     *         total number of errors
     * @param totalHighSeveritySize
     *         total number of warnings with severity high
     * @param totalNormalSeveritySize
     *         total number of warnings with severity normal
     * @param totalLowSeveritySize
     *         total number of warnings with severity low
     */
    public AnalysisScore(final String id, final String displayName,
            final AnalysisConfiguration configuration, final int totalErrorsSize,
            final int totalHighSeveritySize, final int totalNormalSeveritySize, final int totalLowSeveritySize) {
        super(id, displayName);

        this.errorsSize = totalErrorsSize;
        this.highSeveritySize = totalHighSeveritySize;
        this.normalSeveritySize = totalNormalSeveritySize;
        this.lowSeveritySize = totalLowSeveritySize;

        setTotalImpact(computeImpact(configuration));
    }

    private int computeImpact(final AnalysisConfiguration configuration) {
        int change = 0;

        change = change + configuration.getErrorImpact() * getErrorsSize();
        change = change + configuration.getHighImpact() * getHighSeveritySize();
        change = change + configuration.getNormalImpact() * getNormalSeveritySize();
        change = change + configuration.getLowImpact() * getLowSeveritySize();

        return change;
    }

    public final int getErrorsSize() {
        return errorsSize;
    }

    public final int getHighSeveritySize() {
        return highSeveritySize;
    }

    public final int getNormalSeveritySize() {
        return normalSeveritySize;
    }

    public final int getLowSeveritySize() {
        return lowSeveritySize;
    }

    public final int getTotalSize() {
        return getErrorsSize() + getHighSeveritySize() + getNormalSeveritySize() + getLowSeveritySize();
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
        AnalysisScore that = (AnalysisScore) o;
        return errorsSize == that.errorsSize
                && highSeveritySize == that.highSeveritySize
                && normalSeveritySize == that.normalSeveritySize
                && lowSeveritySize == that.lowSeveritySize;
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorsSize, highSeveritySize, normalSeveritySize, lowSeveritySize);
    }

    @Override @Generated
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("errorsSize", errorsSize)
                .append("highSeveritySize", highSeveritySize)
                .append("normalSeveritySize", normalSeveritySize)
                .append("lowSeveritySize", lowSeveritySize)
                .toString();
    }

    /**
     * A builder for {@link AnalysisScore} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class AnalysisScoreBuilder {
        private String id = "analysis";
        private String displayName = "Static Analysis";
        private AnalysisConfiguration configuration = new AnalysisConfiguration();

        private int totalErrorsSize;
        private int totalHighSeveritySize;
        private int totalNormalSeveritySize;
        private int totalLowSeveritySize;

        /**
         * Sets the ID of the analysis tool.
         *
         * @param id
         *         the ID
         *
         * @return this
         */
        public AnalysisScoreBuilder withId(final String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the human readable name of the analysis tool.
         *
         * @param displayName
         *         the name to show
         *
         * @return this
         */
        public AnalysisScoreBuilder withDisplayName(final String displayName) {
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
        public AnalysisScoreBuilder withConfiguration(final AnalysisConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Sets the total number of errors.
         *
         * @param totalErrorsSize
         *         total number of errors
         *
         * @return this
         */
        public AnalysisScoreBuilder withTotalErrorsSize(final int totalErrorsSize) {
            this.totalErrorsSize = totalErrorsSize;
            return this;
        }

        /**
         * Sets the total number of warnings with severity high.
         *
         * @param totalHighSeveritySize
         *         total number of warnings with severity high
         *
         * @return this
         */
        public AnalysisScoreBuilder withTotalHighSeveritySize(final int totalHighSeveritySize) {
            this.totalHighSeveritySize = totalHighSeveritySize;
            return this;
        }

        /**
         * Sets the total number of warnings with severity normal.
         *
         * @param totalNormalSeveritySize
         *         total number of warnings with severity normal
         *
         * @return this
         */
        public AnalysisScoreBuilder withTotalNormalSeveritySize(final int totalNormalSeveritySize) {
            this.totalNormalSeveritySize = totalNormalSeveritySize;
            return this;
        }

        /**
         * Sets the total number of warnings with severity low.
         *
         * @param totalLowSeveritySize
         *         total number of warnings with severity low
         *
         * @return this
         */
        public AnalysisScoreBuilder withTotalLowSeveritySize(final int totalLowSeveritySize) {
            this.totalLowSeveritySize = totalLowSeveritySize;
            return this;
        }

        /**
         * Builds the {@link AnalysisScore} instance with the configured values.
         *
         * @return the new instance
         */
        public AnalysisScore build() {
            return new AnalysisScore(id, displayName, configuration, totalErrorsSize, totalHighSeveritySize,
                    totalNormalSeveritySize, totalLowSeveritySize);
        }
    }
}

