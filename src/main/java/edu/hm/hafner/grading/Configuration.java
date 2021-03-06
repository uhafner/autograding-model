package edu.hm.hafner.grading;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Generated;

/**
 * Base class for configurations with a maximum score.
 *
 * @author Ullrich Hafner
 */
public abstract class Configuration implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean enabled;
    private int maxScore;

    static final JacksonFacade JACKSON_FACADE = new JacksonFacade();

    Configuration(final int maxScore) {
        this(maxScore, true);
    }

    Configuration() {
        this(0, false);
    }

    private Configuration(final int maxScore, final boolean isEnabled) {
        enabled = isEnabled;
        this.maxScore = maxScore;
    }

    public final void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns whether the impact of all properties is positive or negative.
     *
     * @return {@code true} if the impact is positive, {@code false} if the impact is negative
     */
    public abstract boolean isPositive();

    @JsonIgnore
    public final boolean isDisabled() {
        return !enabled;
    }

    public final void setMaxScore(final int maxScore) {
        this.maxScore = maxScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    @Override @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Configuration that = (Configuration) o;
        return maxScore == that.maxScore;
    }

    @Override @Generated
    public int hashCode() {
        return Objects.hash(maxScore);
    }

    @Override
    public String toString() {
        return JACKSON_FACADE.toJson(this);
    }

    /**
     * Base class for builders of {@link Configuration} instances.
     *
     * @author Ullrich Hafner
     */
    static class ConfigurationBuilder {
        private int maxScore;

        /**
         * Sets the maximum score to achieve for the test results.
         *
         * @param maxScore
         *         maximum score to achieve for the test results.
         *
         * @return this
         */
        public ConfigurationBuilder setMaxScore(final int maxScore) {
            this.maxScore = maxScore;

            return this;
        }

        int getMaxScore() {
            return maxScore;
        }
    }
}
