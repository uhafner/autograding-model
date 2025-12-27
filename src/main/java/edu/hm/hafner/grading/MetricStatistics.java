package edu.hm.hafner.grading;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.Value;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Provides statistics about metrics.
 *
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
public class MetricStatistics {
    private final Map<Scope, Map<String, Value>> valuesOfScope = new EnumMap<>(Scope.class);

    /**
     * Adds the specified metric value.
     * The metric id is obtained from the value.
     * The scope is set to default {@link Scope#PROJECT}.
     *
     * @param value
     *         the metric value to add
     *
     * @return this statistics object
     */
    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value) {
        return add(value, Scope.PROJECT);
    }

    /**
     * Adds the specified metric value.
     * The metric id is obtained from the value.
     *
     * @param value
     *         the metric value to add
     * @param scope
     *         the scope of the metric
     *
     * @return this statistics object
     */
    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value, final Scope scope) {
        return add(value, scope, value.getMetric().toTagName());
    }

    /**
     * Adds the specified metric value.
     * The scope is set to default {@link Scope#PROJECT}.
     *
     * @param value
     *         the metric value to add
     * @param id
     *         the scope of the metric
     *
     * @return this statistics object
     */
    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value, final String id) {
        return add(value, Scope.PROJECT, id);
    }

    /**
     * Adds the specified metric value.
     *
     * @param value
     *         the metric value to add
     * @param scope
     *        the scope of the metric
     * @param id
     *         the metric id
     *
     * @return this statistics object
     */
    @CanIgnoreReturnValue
    public MetricStatistics add(final Value value, final Scope scope, final String id) {
        if (hasValue(id, scope)) {
            throw new IllegalArgumentException("Metric " + id + " is already present");
        }
        getValues(scope).put(id, value);

        return this;
    }

    /**
     * Returns the metric value as double value.
     * The scope is set to default {@link Scope#PROJECT}.
     *
     * @param id
     *         the metric id
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */
    public double asDouble(final String id) {
        return asDouble(id, Scope.PROJECT);
    }

    /**
     * Returns the metric value as double value.
     *
     * @param id
     *         the metric id
     * @param scope
     *         the scope of the metric
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */
    public double asDouble(final String id, final Scope scope) {
        return getValue(id, scope).asDouble();
    }

    /**
     * Returns the metric value as a text.
     * The scope is set to default {@link Scope#PROJECT}.
     *
     * @param id
     *         the metric id
     * @param locale
     *         the locale to use
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */
    public String asText(final String id, final Locale locale) {
        return asText(id, locale, Scope.PROJECT);
    }

    /**
     * Returns the metric value as a text.
     *
     * @param id
     *         the metric id
     * @param locale
     *         the locale to use
     * @param scope
     *         the scope of the metric
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */
    public String asText(final String id, final Locale locale, final Scope scope) {
        return getValue(id, scope).asText(locale);
    }

    private Value getValue(final String id, final Scope scope) {
        var values = getValues(scope);
        if (!values.containsKey(id)) {
            throw new NoSuchElementException("Metric " + id + " is not available in scope " + scope + " : " + this);
        }
        return values.get(id);
    }

    /**
     * Returns the metric values as a map from metric id to integer value.
     *
     * @param scope
     *        the scope of the metric
     *
     * @return the metric values
     */
    public Map<String, Double> asMap(final Scope scope) {
        return getValues(scope).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().asRounded()));
    }

    /**
     * Returns the metric values as a map from metric id to integer value.
     *
     * @param scope
     *        the scope of the metric
     *
     * @return the metric values
     */
    public Map<String, String> asFormattedMap(final Scope scope) {
        return getValues(scope).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().asRoundedText(Locale.ENGLISH)));
    }

    private Map<String, Value> getValues(final Scope scope) {
        return valuesOfScope.computeIfAbsent(scope, b -> new HashMap<>());
    }

    /**
     * Returns the metric value as a text.
     * The scope is set to default {@link Scope#PROJECT}.
     *
     * @param id
     *         the metric id
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */
    public boolean hasValue(final String id) {
        return hasValue(id, Scope.PROJECT);
    }

    /**
     * Returns the metric value as a text.
     *
     * @param id
     *         the metric id
     * @param scope
     *         the scope of the metric
     *
     * @return the metric value
     * @throws IllegalArgumentException
     *         if the metric is not available
     */

    public boolean hasValue(final String id, final Scope scope) {
        return getValues(scope).containsKey(id);
    }

    @Override
    public String toString() {
        return valuesOfScope.toString();
    }
}
