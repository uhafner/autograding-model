package edu.hm.hafner.grading;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.util.Generated;

/**
 * Configuration to grade static analysis results.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.DataClass", "HashCodeToString"})
public class AnalysisConfiguration extends Configuration {
    private static final long serialVersionUID = 1L;
    private static final String ANALYSIS_ID = "analysis";

    private int errorImpact;
    private int highImpact;
    private int normalImpact;
    private int lowImpact;

    /**
     * Converts the specified JSON object to a new instance if {@link AnalysisConfiguration}.
     *
     * @param json
     *         the json object to convert
     *
     * @return the corresponding {@link AnalysisConfiguration} instance
     */
    public static AnalysisConfiguration from(final String json) {
        return JACKSON_FACADE.fromJson(json, AnalysisConfiguration.class);
    }

    /**
     * Converts the specified JSON object to a new instance if {@link AnalysisConfiguration}.
     *
     * @param jsonNode
     *         the json object to convert
     *
     * @return the corresponding {@link AnalysisConfiguration} instance
     */
    public static AnalysisConfiguration from(final JsonNode jsonNode) {
        if (jsonNode.has(ANALYSIS_ID)) {
            JsonNode node = jsonNode.get(ANALYSIS_ID);
            AnalysisConfiguration configuration = JACKSON_FACADE.fromJson(node, AnalysisConfiguration.class);
            configuration.setEnabled(true);

            return configuration;
        }
        return new AnalysisConfiguration();
    }

    /**
     * Creates a configuration that suppresses the grading.
     */
    @SuppressWarnings("unused") // Required for JSON conversion
    public AnalysisConfiguration() {
        super();
    }

    AnalysisConfiguration(final int maxScore,
            final int errorImpact, final int highImpact, final int normalImpact, final int lowImpact) {
        super(maxScore);

        this.errorImpact = errorImpact;
        this.highImpact = highImpact;
        this.normalImpact = normalImpact;
        this.lowImpact = lowImpact;
    }

    @Override
    public boolean isPositive() {
        return errorImpact >= 0 && highImpact >= 0 && normalImpact >= 0 && lowImpact >= 0;
    }

    public int getErrorImpact() {
        return errorImpact;
    }

    @SuppressWarnings("unused") // Required for JSON conversion
    public void setErrorImpact(final int errorImpact) {
        this.errorImpact = errorImpact;
    }

    public int getHighImpact() {
        return highImpact;
    }

    @SuppressWarnings("unused") // Required for JSON conversion
    public void setHighImpact(final int highImpact) {
        this.highImpact = highImpact;
    }

    public int getNormalImpact() {
        return normalImpact;
    }

    @SuppressWarnings("unused") // Required for JSON conversion
    public void setNormalImpact(final int weightNormal) {
        normalImpact = weightNormal;
    }

    @SuppressWarnings("unused") // Required for JSON conversion
    public void setLowImpact(final int lowImpact) {
        this.lowImpact = lowImpact;
    }

    public int getLowImpact() {
        return lowImpact;
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
        AnalysisConfiguration that = (AnalysisConfiguration) o;
        return errorImpact == that.errorImpact
                && highImpact == that.highImpact
                && normalImpact == that.normalImpact
                && lowImpact == that.lowImpact;
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorImpact, highImpact, normalImpact, lowImpact);
    }

    /**
     * Builder to create a {@link AnalysisConfiguration} instance.
     */
    public static class AnalysisConfigurationBuilder extends ConfigurationBuilder {
        private int errorImpact;
        private int highImpact;
        private int normalImpact;
        private int lowImpact;

        @Override @CanIgnoreReturnValue
        public AnalysisConfigurationBuilder setMaxScore(final int maxScore) {
            return (AnalysisConfigurationBuilder) super.setMaxScore(maxScore);
        }

        /**
         * Sets the number of points to increase or decrease the score if an error has been detected.
         *
         * @param errorImpact
         *         number of points to increase or decrease the score if an error has been detected
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisConfigurationBuilder setErrorImpact(final int errorImpact) {
            this.errorImpact = errorImpact;
            return this;
        }

        /**
         * Sets the number of points to increase or decrease the score if a warning with severity high has been
         * detected.
         *
         * @param highImpact
         *         number of points to increase or decrease the score if a warning with severity high has been detected
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisConfigurationBuilder setHighImpact(final int highImpact) {
            this.highImpact = highImpact;
            return this;
        }

        /**
         * Sets the number of points to increase or decrease the score if a warning with severity normal has been
         * detected.
         *
         * @param normalImpact
         *         number of points to increase or decrease the score if a warning with severity normal has been
         *         detected
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisConfigurationBuilder setNormalImpact(final int normalImpact) {
            this.normalImpact = normalImpact;
            return this;
        }

        /**
         * Sets the number of points to increase or decrease the score if a warning with severity low has been
         * detected.
         *
         * @param weightLow
         *         number of points to increase or decrease the score if a warning with severity low has been detected
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public AnalysisConfigurationBuilder setLowImpact(final int weightLow) {
            lowImpact = weightLow;
            return this;
        }

        /**
         * Creates a new instance of {@link AnalysisConfiguration} using the configured properties.
         *
         * @return the created instance
         */
        public AnalysisConfiguration build() {
            return new AnalysisConfiguration(getMaxScore(), errorImpact, highImpact, normalImpact, lowImpact);
        }
    }
}
