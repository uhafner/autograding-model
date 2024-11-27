package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.hm.hafner.util.Generated;

/**
 * Configuration to grade code coverage results. The configuration specifies the impact of the coverage results on the
 * score. This class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.DataClass", "HashCodeToString"})
public abstract class CoverageModelConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;

    @SuppressWarnings("unused") // JSON property
    private String pattern;
    @SuppressWarnings("unused") // JSON property
    private String parserId;
    private final List<CoverageParserConfiguration> metrics = new ArrayList<>();

    CoverageModelConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    public String getPattern() {
        return StringUtils.defaultString(pattern);
    }

    public String getParserId() {
        return Objects.toString(parserId, getDefaultParserId());
    }

    /**
     * Returns a default ID for this configuration.
     *
     * @return the default ID of this configuration
     */
    @JsonIgnore
    protected abstract String getDefaultParserId();

    public List<CoverageParserConfiguration> getMetrics() {
        return metrics;
    }

    @Override
    protected void validate() {
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("Configuration '" + getName() + "' has no metrics");
        }
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        var that = (CoverageModelConfiguration) o;
        return Objects.equals(pattern, that.pattern)
                && Objects.equals(metrics, that.metrics);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), pattern, metrics);
    }
}
