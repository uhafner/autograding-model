package edu.hm.hafner.grading;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.Generated;

import one.util.streamex.StreamEx;

/**
 * Configuration for quality gates that determine build success/failure based on metrics.
 * This class follows the same Jackson deserialization pattern as other configurations.
 */
public final class QualityGatesConfiguration {
    private static final String QUALITY_GATES_ID = "qualityGates";

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
     * Parses quality gates from an environment variable with comprehensive logging.
     * This is a convenience method for CI environments that pass configuration via environment variables.
     *
     * @param envVarName the name of the environment variable containing JSON configuration
     * @param log the logger for detailed feedback  
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
        } catch (Exception exception) {
            log.logException(exception, "Error parsing quality gates JSON configuration");
            return List.of();
        }
    }

    /**
     * Extracts quality gates from JSON using the same pattern as Configuration.extractConfigurations.
     *
     * @param json the JSON string
     * @param id the JSON property name to extract
     * @return list of QualityGate objects
     */
    static List<QualityGate> extractQualityGates(final String json, final String id) {
        var jackson = JacksonFacade.get();

        var configurations = jackson.readJson(json);
        if (configurations.has(id)) {
            var deserialized = deserializeQualityGates(id, configurations, jackson);
            // Validate each quality gate
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
        if (gate.getThreshold() <= 0.0) {
            throw new IllegalArgumentException(
                    String.format("Quality gate threshold must be positive: %.2f for metric %s", 
                            gate.getThreshold(), gate.getMetric()));
        }
    }

    /**
     * DTO class for Jackson deserialization of individual quality gates.
     */
    static class QualityGateDto {
        @JsonProperty("metric")
        private String metric = "";
        
        @JsonProperty("threshold")
        private double threshold = 0.0;
        
        @JsonProperty("criticality")
        private String criticality = "UNSTABLE";

        @JsonProperty("baseline")
        private String baseline = "PROJECT";

        @JsonProperty("name")
        private String name = "";

        @JsonIgnore
        public QualityGate toQualityGate() {
            var parsedCriticality = parseCriticality(criticality);
            
            // Generate display name if not provided
            var displayName = StringUtils.isBlank(name) 
                    ? generateDisplayName(metric) 
                    : name;
            
            return new QualityGate(displayName, metric, threshold, parsedCriticality);
        }

        private QualityGate.Criticality parseCriticality(final String criticalityStr) {
            try {
                return QualityGate.Criticality.valueOf(criticalityStr.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException exception) {
                // Default to UNSTABLE for unknown criticality
                return QualityGate.Criticality.UNSTABLE;
            }
        }

        private String generateDisplayName(final String metricName) {
            if (StringUtils.isBlank(metricName)) {
                return "Quality Gate";
            }

            // Convert metric name to readable format
            String readable = metricName.toLowerCase(Locale.ROOT)
                    .replace("_", " ")
                    .replace("-", " ");
            
            // Capitalize first letter of each word
            var words = readable.split("\\s+");
            var result = new StringBuilder();
            
            for (var word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1));
                    }
                }
            }
            
            return result.toString();
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

    // Private constructor - this is a utility class
    private QualityGatesConfiguration() {
        // Utility class
    }
} 