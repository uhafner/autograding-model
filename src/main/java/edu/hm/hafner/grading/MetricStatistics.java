package edu.hm.hafner.grading;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides statistics about metrics.
 *
 * @author Ullrich Hafner
 */
public class MetricStatistics {
    private final Map<Scope, Map<String, Value>> projectValues = new HashMap<>();
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
    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value) {
        return add(value, Scope.PROJECT, value.getMetric().toTagName());
    }

    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value, final Scope scope) {
        return add(value, scope, value.getMetric().toTagName());
    }

    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value, final String id) {
        return add(value, Scope.PROJECT, id);
    }

    /**
     * Adds the specified metric value.
     *
     * @param value
     *         the metric value to add
     * @param id
     *         the metric id
     *
     * @return this statistics object
     */
    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value, final Scope scope, final String id) {
        var values = projectValues.computeIfAbsent(scope, b -> new HashMap<>());
        if (values.containsKey(id)) {
            throw new IllegalArgumentException("Metric " + id + " is already present");
        }
        values.put(id, value);

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
        return this.asDouble(id, Scope.PROJECT);
    }

    public double asDouble(final String id, final Scope scope) {
        var values = projectValues.computeIfAbsent(scope, b -> new HashMap<>());
        if (!values.containsKey(id)) {
            throw new IllegalArgumentException("Metric " + id + " is not available in " + this);
        }
        return values.get(id).asDouble();
    }

    /**
     * Returns the metric values as a map from metric id to integer value.
     *
     * @return the metric values
     */
    public Map<String, Double> asMap(final Scope scope) {
        var values = projectValues.computeIfAbsent(scope, b -> new HashMap<>());
        return values.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().asDouble()));
    }

    @Override
    public String toString() {
        return projectValues.toString();
    }
}
