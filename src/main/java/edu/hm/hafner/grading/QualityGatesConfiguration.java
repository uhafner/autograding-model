package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import one.util.streamex.StreamEx;

/**
 * Configuration for quality gates that determine build success/failure based on metrics. This class follows the same
 * Jackson deserialization pattern as other configurations.
 */
public final class QualityGatesConfiguration {
    private static final String QUALITY_GATES_ID = "qualityGates";

    private static final ParserRegistry PARSER_REGISTRY = new ParserRegistry();

    /**
     * Converts the specified JSON object to a list of {@link QualityGate} instances.
     *
     * @param json
     *         the JSON object to convert
     *
     * @return the corresponding {@link QualityGate} instances
     */
    public static List<QualityGate> from(final String json) {
        return extractQualityGates(json, QUALITY_GATES_ID);
    }

    /**
     * Parses quality gates from an environment variable with comprehensive logging. This is a convenience method for CI
     * environments that pass configuration via environment variables.
     *
     * @param envVarName
     *         the name of the environment variable containing JSON configuration
     * @param log
     *         the logger for detailed feedback
     *
     * @return the list of quality gates, or empty list if not found or parsing fails
     */
    public static List<QualityGate> parseFromEnvironment(final String envVarName, final FilteredLog log) {
        String json = System.getenv(envVarName);
        if (StringUtils.isBlank(json)) {
            log.logInfo("Environment variable '%s' not found or empty", envVarName);
            return List.of();
        }

        log.logInfo("Found quality gates configuration in environment variable '%s'", envVarName);
        log.logInfo("Parsing quality gates from JSON configuration using QualityGatesConfiguration");

        try {
            var qualityGates = from(json);
            log.logInfo("Parsed %d quality gate(s) from JSON configuration", qualityGates.size());
            return qualityGates;
        }
        catch (IllegalArgumentException exception) {
            log.logException(exception, "Error parsing quality gates JSON configuration");
            return List.of();
        }
    }

    /**
     * Extracts quality gates from JSON using the same pattern as Configuration.extractConfigurations.
     *
     * @param json
     *         the JSON string
     * @param id
     *         the JSON property name to extract
     *
     * @return list of QualityGate objects
     */
    static List<QualityGate> extractQualityGates(final String json, final String id) {
        var jackson = JacksonFacade.get();

        var configurations = jackson.readJson(json);
        if (configurations.has(id)) {
            var deserialized = deserializeQualityGates(id, configurations, jackson);
            deserialized.forEach(QualityGatesConfiguration::validateQualityGate);
            return deserialized;
        }
        return Collections.emptyList();
    }

    private static List<QualityGate> deserializeQualityGates(final String id, final JsonNode configurations,
            final JacksonFacade jackson) {
        var array = configurations.get(id);

        if (array.isArray()) {
            return StreamEx.of(array.iterator())
                    .map(node -> jackson.fromJson(node, QualityGateDto.class))
                    .map(QualityGateDto::toQualityGate)
                    .toList();
        }
        return List.of(jackson.fromJson(array, QualityGateDto.class).toQualityGate());
    }

    private static void validateQualityGate(final QualityGate gate) {
        if (StringUtils.isBlank(gate.getMetric())) {
            throw new IllegalArgumentException("Quality gate metric cannot be blank: " + gate);
        }
        if (gate.getThreshold() < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ENGLISH, "Quality gate threshold must be not negative: %.2f for metric %s",
                            gate.getThreshold(), gate.getMetric()));
        }
    }

    private QualityGatesConfiguration() {
        // Utility class
    }

    /**
     * DTO class for Jackson deserialization of individual quality gates.
     */
    @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal", "PMD.DataClass", "PMD.ImmutableField", "FieldCanBeLocal"})
    static class QualityGateDto {
        private String metric = "";
        private double threshold = 0.0;
        private String criticality = "UNSTABLE";
        private String baseline = "PROJECT";
        private String name = "";

        @JsonIgnore
        public QualityGate toQualityGate() {
            if (StringUtils.isBlank(metric)) {
                throw new IllegalArgumentException("Quality gate metric cannot be blank");
            }

            var parsedCriticality = parseCriticality(criticality);

            // Generate display name if not provided
            var displayName = StringUtils.isBlank(name) ? generateDisplayName() : name;

            return new QualityGate(displayName, metric, threshold, parsedCriticality);
        }

        private QualityGate.Criticality parseCriticality(final String criticalityStr) {
            return QualityGate.Criticality.valueOf(criticalityStr.toUpperCase(Locale.ROOT));
        }

        private String generateDisplayName() {
            if (PARSER_REGISTRY.contains(metric)) {
                return PARSER_REGISTRY.get(metric).getName();
            }

            try {
                return Metric.fromName(metric).getDisplayName();
            }
            catch (IllegalArgumentException e) {
                // Handle -modified suffix for coverage metrics (e.g., line-modified, branch-modified)
                if (metric.endsWith("-modified")) {
                    String baseMetric = metric.substring(0, metric.length() - "-modified".length());
                    try {
                        String baseDisplayName = Metric.fromName(baseMetric).getDisplayName();
                        return baseDisplayName + " (Modified Lines)";
                    }
                    catch (IllegalArgumentException baseException) {
                        // Base metric not found, use formatted version of metric name
                        return formatMetricName(baseMetric) + " (Modified Lines)";
                    }
                }
                // If a metric is not recognized, use the metric enum as the display name
                return metric;
            }
        }

        private String formatMetricName(final String metricName) {
            // Convert "line" to "Line", "branch" to "Branch", etc.
            if (metricName.isEmpty()) {
                return metricName;
            }
            return metricName.substring(0, 1).toUpperCase(Locale.ROOT) + metricName.substring(1);
        }


        // Getters for Jackson (required for deserialization)
        public String getMetric() {
            return metric;
        }

        public double getThreshold() {
            return threshold;
        }

        public String getCriticality() {
            return criticality;
        }

        public String getBaseline() {
            return baseline;
        }

        public String getName() {
            return name;
        }

        @Override
        @Generated
        public String toString() {
            return String.format("QualityGateDto{metric='%s', threshold=%.2f, criticality='%s'}",
                    metric, threshold, criticality);
        }
    }
}
