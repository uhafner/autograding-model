package edu.hm.hafner.grading;

import java.io.Serial;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Configuration to grade software metrics. The configuration specifies the impact of the software metrics results
 * on the score. This class is intended to be deserialized from JSON, there is no public constructor available.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("unused")
public final class MetricConfiguration extends Configuration {
    @Serial
    private static final long serialVersionUID = 3L;

    private static final String METRICS_ID = "metrics";

    /**
     * Converts the specified JSON object to a list of {@link MetricConfiguration} instances.
     *
     * @param json
     *         the JSON object to convert
     *
     * @return the corresponding {@link MetricConfiguration} instances
     */
    public static List<MetricConfiguration> from(final String json) {
        return extractConfigurations(json, METRICS_ID, MetricConfiguration.class);
    }

    private MetricConfiguration() {
        super(); // Instances are created via JSON deserialization
    }

    @Override
    protected String getDefaultName() {
        return "Metrics";
    }

    @Override
    @JsonIgnore
    public boolean isPositive() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean hasImpact() {
        return false;
    }
}
