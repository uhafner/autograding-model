package edu.hm.hafner.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.DefaultLocale;

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
                                      "threshold": 0.0,
                                      "criticality": "FAILURE"
                                    }
                                  ]
                                }
                                """, "Quality gate threshold must be positive: 0.00 for metric line",
                        "zero threshold"),
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
                                """, "Quality gate threshold must be positive: -10.00 for metric line",
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
        var gate = qualityGates.get(0);
        assertThat(gate.getMetric()).isEqualTo("line");
        assertThat(gate.getThreshold()).isEqualTo(80.0);
        assertThat(gate.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(gate.getName()).isEqualTo("Line Coverage Gate");
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

        var lineGate = qualityGates.get(0);
        assertThat(lineGate.getMetric()).isEqualTo("line");
        assertThat(lineGate.getThreshold()).isEqualTo(80.0);
        assertThat(lineGate.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(lineGate.getName()).isEqualTo("Line Coverage");

        var branchGate = qualityGates.get(1);
        assertThat(branchGate.getMetric()).isEqualTo("branch");
        assertThat(branchGate.getThreshold()).isEqualTo(70.0);
        assertThat(branchGate.getCriticality()).isEqualTo(QualityGate.Criticality.UNSTABLE);
        assertThat(branchGate.getName()).isEqualTo("Branch Coverage");
    }

    @Test
    void shouldGenerateDisplayNameWhenNotProvided() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line_coverage",
                      "threshold": 80.0,
                      "criticality": "FAILURE"
                    },
                    {
                      "metric": "branch-coverage",
                      "threshold": 70.0,
                      "criticality": "UNSTABLE"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(2);
        assertThat(qualityGates.get(0).getName()).isEqualTo("Line Coverage");
        assertThat(qualityGates.get(1).getName()).isEqualTo("Branch Coverage");
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

        assertThat(qualityGates).hasSize(1);
        assertThat(qualityGates.get(0).getCriticality()).isEqualTo(QualityGate.Criticality.UNSTABLE);
    }

    @Test
    void shouldHandleInvalidCriticalityGracefully() {
        var qualityGates = QualityGatesConfiguration.from("""
                {
                  "qualityGates": [
                    {
                      "metric": "line",
                      "threshold": 80.0,
                      "criticality": "INVALID_VALUE"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(1);
        assertThat(qualityGates.get(0).getCriticality()).isEqualTo(QualityGate.Criticality.UNSTABLE);
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
                .containsExactly(QualityGate.Criticality.UNSTABLE, QualityGate.Criticality.FAILURE);
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
        assertThat(qualityGates.get(0).getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
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
                      "baseline": "PROJECT"
                    },
                    {
                      "metric": "checkstyle",
                      "threshold": 10.0,
                      "criticality": "UNSTABLE",
                      "name": "Style Issues"
                    }
                  ]
                }
                """);

        assertThat(qualityGates).hasSize(2);

        var lineGate = qualityGates.get(0);
        assertThat(lineGate.getMetric()).isEqualTo("line");
        assertThat(lineGate.getThreshold()).isEqualTo(80.0);
        assertThat(lineGate.getCriticality()).isEqualTo(QualityGate.Criticality.FAILURE);
        assertThat(lineGate.getName()).isEqualTo("Line Coverage Gate");

        var styleGate = qualityGates.get(1);
        assertThat(styleGate.getMetric()).isEqualTo("checkstyle");
        assertThat(styleGate.getThreshold()).isEqualTo(10.0);
        assertThat(styleGate.getCriticality()).isEqualTo(QualityGate.Criticality.UNSTABLE);
        assertThat(styleGate.getName()).isEqualTo("Style Issues");
    }
}
