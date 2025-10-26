package edu.hm.hafner.grading;

import edu.hm.hafner.coverage.Value;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides statistics about metrics.
 *
 * @author Ullrich Hafner
 */
public class MetricStatistics {
    private final Map<String, Value> projectValues = new java.util.HashMap<>();
    // TODO: we might need values per baseline, see
    // https://github.com/jenkinsci/coverage-plugin/blob/main/plugin/src/main/java/io/jenkins/plugins/coverage/metrics/model/CoverageStatistics.java

    /**
     * Adds the specified metric value. The metric id is obtained from the value.
     *
     * @param value
     *         the metric value to add
     *
     * @return this statistics object
     */
    public MetricStatistics add(final Value value) {
        return add(value.getMetric().toTagName(), value);
    }

    /**
     * Adds the specified metric value.
     *
     * @param id
     *         the metric id
     * @param value
     *         the metric value to add
     *
     * @return this statistics object
     */
    public MetricStatistics add(final String id, final Value value) {
        if (projectValues.containsKey(id)) {
            throw new IllegalArgumentException("Metric " + id + " is already present");
        }
        projectValues.put(id, value);

        return this;
    }

    /**
     * Returns the metric value as double value.
     *
     * @param id
     *         the metric id
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */
    public double asDouble(final String id) {
        if (!projectValues.containsKey(id)) {
            throw new IllegalArgumentException("Metric " + id + " is not available in " + this);
        }
        return projectValues.get(id).asDouble();
    }

    /**
     * Returns the metric values as a map from metric id to integer value.
     *
     * @return the metric values
     */
    public Map<String, Integer> asMap() {
        return projectValues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asInteger()));
    }

    @Override
    public String toString() {
        return projectValues.toString();
    }
}
