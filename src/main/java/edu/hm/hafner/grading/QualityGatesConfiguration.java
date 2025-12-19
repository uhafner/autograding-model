package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.FilteredLog;

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

    static List<QualityGate> parseQualityGates(final String json, final FilteredLog log) {
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
            return deserializeQualityGates(id, configurations, jackson);
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
        private String scope = "project";
        private String name = "";

        @JsonIgnore
        public QualityGate toQualityGate() {
            if (StringUtils.isBlank(metric)) {
                throw new IllegalArgumentException("Quality gate metric cannot be blank");
            }

            var parsedCriticality = parseCriticality(criticality);

            var displayName = StringUtils.defaultIfBlank(name, generateDisplayName());

            if (threshold < 0) {
                throw new IllegalArgumentException(
                        String.format(Locale.ENGLISH, "Quality gate threshold must be not negative: %.2f for metric %s",
                                threshold, metric));
            }

            return new QualityGate(displayName, metric, Scope.fromString(scope), threshold, parsedCriticality);
        }

        private QualityGate.Criticality parseCriticality(final String criticalityStr) {
            return QualityGate.Criticality.valueOf(criticalityStr.toUpperCase(Locale.ROOT));
        }

        private String generateDisplayName() {
            return "%s (%s)".formatted(detectMetricName(), Scope.fromString(scope).getDisplayName());
        }

        private String detectMetricName() {
            if (PARSER_REGISTRY.contains(metric)) {
                return PARSER_REGISTRY.get(metric).getName();
            }

            try {
                return Metric.fromName(metric).getDisplayName();
            }
            catch (IllegalArgumentException e) {
                // If a metric is not recognized, use the metric value as the display name
                return metric;
            }
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

        public String getScope() {
            return scope;
        }

        public String getName() {
            return name;
        }
    }
}
