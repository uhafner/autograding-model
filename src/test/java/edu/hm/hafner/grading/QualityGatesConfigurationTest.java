package edu.hm.hafner.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.grading.QualityGate.Criticality;
import edu.hm.hafner.util.FilteredLog;

import java.util.stream.Stream;

import static edu.hm.hafner.grading.assertions.Assertions.*;

@DefaultLocale("en")
class QualityGatesConfigurationTest {
    @ParameterizedTest(name = "{index} => Invalid configuration: {2}")
    @MethodSource
    @DisplayName("should throw exceptions for invalid configurations")
    void shouldReportNotConsistentConfiguration(final String json, final String errorMessage,
            @SuppressWarnings("unused") final String displayName) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QualityGatesConfiguration.from(json))
                .withMessageContaining(errorMessage)
                .withNoCause();
    }

    public static Stream<Arguments> shouldReportNotConsistentConfiguration() {
        return Stream.of(
                Arguments.of("""
                                {
                                  "qualityGates": [
                                    {
                                      "threshold": 80.0,
                                      "criticality": "FAILURE"
                                    }
                                  ]
                                }
                                """, "Quality gate metric cannot be blank",
                        "missing metric"),
                Arguments.of("""
                                {
                                  "qualityGates": [
                                    {
                                      "metric": "",
                                      "threshold": 80.0,
                                      "criticality": "FAILURE"
                                    }
                                  ]
                                }
                                """, "Quality gate metric cannot be blank",
                        "empty metric"),
                Arguments.of("""
                                {
                                  "qualityGates": [
                                    {
                                      "metric": "line",
                                      "threshold": -10.0,
                                      "criticality": "FAILURE"
                                    }
                                  ]
                                }
                                """, "Quality gate threshold must be not negative: -10.00 for metric line",
                        "negative threshold")
        );
    }

    @Test
    void shouldConvertSingleQualityGateFromJson() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": {
                    "metric": "line",
                    "threshold": 80.0,
                    "criticality": "FAILURE",
                    "name": "Line Coverage Gate"
                  }
                }
                """);

        assertThat(qualityGates).hasSize(1);
        var gate = qualityGates.getFirst();
        assertThat(gate.getMetric()).isEqualTo("line");
        assertThat(gate.getThreshold()).isEqualTo(80.0);
        assertThat(gate.getCriticality()).isEqualTo(Criticality.FAILURE);
        assertThat(gate.getName()).isEqualTo("Line Coverage Gate");
    }

    @Test
    void shouldUnknownMetric() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": {
                    "metric": "other",
                    "threshold": 100.0,
                    "name": "Other Metric"
                  }
                }
                """);

        assertThat(qualityGates).hasSize(1);
        var gate = qualityGates.getFirst();
        assertThat(gate.getMetric()).isEqualTo("other");
        assertThat(gate.getThreshold()).isEqualTo(100.00);
        assertThat(gate.getCriticality()).isEqualTo(Criticality.UNSTABLE);
        assertThat(gate.getName()).isEqualTo("Other Metric");
    }

    @Test
    void shouldConvertArrayOfQualityGatesFromJson() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line",
                      "threshold": 80.0,
                      "criticality": "FAILURE",
                      "name": "Line Coverage"
                    },
                    {
                      "metric": "branch",
                      "threshold": 70.0,
                      "criticality": "UNSTABLE",
                      "name": "Branch Coverage"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(2);

        var lineGate = qualityGates.getFirst();
        assertThat(lineGate.getMetric()).isEqualTo("line");
        assertThat(lineGate.getThreshold()).isEqualTo(80.0);
        assertThat(lineGate.getCriticality()).isEqualTo(Criticality.FAILURE);
        assertThat(lineGate.getName()).isEqualTo("Line Coverage");

        var branchGate = qualityGates.get(1);
        assertThat(branchGate.getMetric()).isEqualTo("branch");
        assertThat(branchGate.getThreshold()).isEqualTo(70.0);
        assertThat(branchGate.getCriticality()).isEqualTo(Criticality.UNSTABLE);
        assertThat(branchGate.getName()).isEqualTo("Branch Coverage");
    }

    @Test
    void shouldGenerateDisplayNameWhenNotProvided() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line",
                      "threshold": 80.0,
                      "criticality": "FAILURE"
                    },
                    {
                      "metric": "checkstyle",
                      "threshold": 70.0,
                      "criticality": "UNSTABLE"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(2).map(QualityGate::getName)
                .containsExactly("Line Coverage (Whole Project)", "CheckStyle (Whole Project)");
    }

    @Test
    void shouldUseDefaultCriticalityWhenNotProvided() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line",
                      "threshold": 80.0
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(1).map(QualityGate::getCriticality)
                .containsExactly(Criticality.UNSTABLE);
    }

    @Test
    void shouldHandleInvalidCriticalityGracefully() {
        assertThatIllegalArgumentException().isThrownBy(() -> QualityGatesConfiguration.from("""
                {
                  "qualityGates": [{
                      "metric": "line",
                      "threshold": 80.0,
                      "criticality": "INVALID_VALUE"
                    }
                  ]
                }
                """));
        assertThatIllegalArgumentException().isThrownBy(() -> QualityGatesConfiguration.from("""
                {
                  "qualityGates": [{
                      "threshold": 80.0
                    }
                  ]
                }
                """));
    }

    @Test
    void shouldReturnEmptyListWhenNoQualityGatesProperty() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "other": "configuration"
                }
                """);

        assertThat(qualityGates).isEmpty();
    }

    @Test
    void shouldHandleAllCriticalityLevels() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "branch",
                      "threshold": 70.0,
                      "criticality": "UNSTABLE"
                    },
                    {
                      "metric": "mutation",
                      "threshold": 50.0,
                      "criticality": "FAILURE"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(2)
                .map(QualityGate::getCriticality)
                .containsExactly(Criticality.UNSTABLE, Criticality.FAILURE);
    }

    @Test
    void shouldParseQualityGates() {
        var log = new FilteredLog();
        var qualityGates = QualityGatesConfiguration.parseQualityGates("""
                {
                  "qualityGates": [
                    {
                      "metric": "branch",
                      "threshold": 70.0,
                      "criticality": "UNSTABLE"
                    },
                    {
                      "metric": "mutation",
                      "threshold": 50.0,
                      "criticality": "FAILURE"
                    }
                  ]
                }
                """, log);

        assertThat(qualityGates).hasSize(2)
                .map(QualityGate::getCriticality)
                .containsExactly(Criticality.UNSTABLE, Criticality.FAILURE);
        assertThat(log.getInfoMessages()).containsExactly(
                "Parsing quality gates from JSON configuration using QualityGatesConfiguration",
                "Parsed 2 quality gate(s) from JSON configuration");
    }

    @Test
    void shouldLogErrorWhileParsing() {
        var log = new FilteredLog();
        var qualityGates = QualityGatesConfiguration.parseQualityGates("""
                {
                  "qualityGates": [
                    {
                      broken json
                    }
                """, log);

        assertThat(qualityGates).hasSize(0);
        assertThat(log.getInfoMessages()).containsExactly(
                "Parsing quality gates from JSON configuration using QualityGatesConfiguration");
        assertThat(log.getErrorMessages()).contains(
                "Error parsing quality gates JSON configuration");
    }

    @Test
    void shouldHandleLowercaseCriticality() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line",
                      "threshold": 80.0,
                      "criticality": "failure"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(1);
        assertThat(qualityGates.getFirst().getCriticality()).isEqualTo(Criticality.FAILURE);
    }

    @Test
    void shouldHandleComplexConfiguration() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line",
                      "threshold": 80.0,
                      "criticality": "FAILURE",
                      "name": "Line Coverage Gate",
                      "scope": "project"
                    },
                    {
                      "metric": "checkstyle",
                      "threshold": 0.0,
                      "criticality": "UNSTABLE",
                      "name": "Style Issues"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(2).satisfiesExactly(
                q -> assertThat(q).hasMetric("line")
                        .hasThreshold(80.0)
                        .hasCriticality(Criticality.FAILURE)
                        .hasName("Line Coverage Gate"),
                q -> assertThat(q).hasMetric("checkstyle")
                        .hasThreshold(0.0)
                        .hasCriticality(Criticality.UNSTABLE)
                        .hasName("Style Issues"));
    }
}
