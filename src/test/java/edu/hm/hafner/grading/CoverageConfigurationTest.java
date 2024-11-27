package edu.hm.hafner.grading;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.hm.hafner.coverage.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class CoverageConfigurationTest extends AbstractConfigurationTest {
    @Override
    protected List<CoverageConfiguration> fromJson(final String json) {
        return CoverageConfiguration.from(json);
    }

    @Override
    protected String getInvalidJson() {
        return """
                {
                  "coverage": {
                    "name": "Coverage",
                    "maxScore": 50,
                    "coveredPercentageImpact": 1
                  },
                };
                """;
    }

    @ParameterizedTest(name = "{index} => Invalid configuration: {2}")
    @MethodSource
    @DisplayName("should throw exceptions for invalid configurations")
    void shouldReportNotConsistentConfiguration(final String json, final String errorMessage,
            @SuppressWarnings("unused") final String displayName) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> fromJson(json))
                .withMessageContaining(errorMessage)
                .withNoCause();
    }

    public static Stream<Arguments> shouldReportNotConsistentConfiguration() {
        return Stream.of(
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "id": "jacoco",
                    "pattern": "target/jacoco.xml",
                    "tools": [
                      {
                        "metric": "line"
                      }
                    ],
                    "maxScore": 0,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2
                  }
                }
                """, "When configuring impacts then the score must not be zero.",
                        "an impact requires a positive score"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "id": "jacoco",
                    "pattern": "target/jacoco.xml",
                    "tools": [
                      {
                        "metric": "line"
                      }
                    ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 0,
                    "missedPercentageImpact": 0
                  }
                }
                """, "When configuring a max score than an impact must be defined as well",
                        "a score requires an impact"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 1
                  }
                }
                """, "Configuration ID 'coverage' has no metrics",
                        "empty metrics configuration")
        );
    }

    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "maxScore": 50,
                    "id": "jacoco",
                    "pattern": "target/jacoco.xml",
                    "metrics": [
                      {
                        "name": "JaCoCo",
                        "icon": "jacoco.png",
                        "metric": "line"
                      },
                      {
                        "name": "PITest",
                        "icon": "pit.png",
                        "metric": "mutation"
                      }
                    ],
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2
                  }
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(configuration -> assertThat(configuration)
                .hasCoveredPercentageImpact(1).hasMissedPercentageImpact(2)
                .hasMaxScore(50)
                .hasName("JaCoCo and PIT")
                .isPositive().hasImpact()
                .hasOnlyMetrics(
                        new CoverageParserConfiguration("JaCoCo", "jacoco.png", getMetricName(Metric.LINE)),
                        new CoverageParserConfiguration("PITest", "pit.png", getMetricName(Metric.MUTATION))));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "coverage": [{
                    "id": "jacoco",
                    "pattern": "target/jacoco.xml",
                    "metrics": [
                      {
                        "name": "JaCoCo",
                        "icon": "jacoco.png",
                        "metric": "line"
                      },
                      {
                        "name": "PITest",
                        "icon": "pit.png",
                        "metric": "mutation"
                      }
                    ],
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2,
                    "maxScore": 50
                  }]
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(this::verifyFirstConfiguration);
    }

    @Test
    void shouldConvertMultipleElementsConfigurationsFromJson() {
        var configurations = fromJson("""
                {
                  "coverage": [
                    {
                    "id": "jacoco",
                    "pattern": "target/jacoco.xml",
                    "metrics": [
                      {
                        "name": "JaCoCo",
                        "icon": "jacoco.png",
                        "metric": "line"
                      },
                      {
                        "name": "PITest",
                        "icon": "pit.png",
                        "metric": "mutation"
                      }
                    ],
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2,
                    "maxScore": 50
                  },
                  {
                    "id": "cobertura",
                    "pattern": "target/cobertura.xml",
                    "metrics": [
                      {
                        "name": "Cobertura",
                        "icon": "cobertura.png",
                        "metric": "branch"
                      }
                    ],
                    "coveredPercentageImpact": -5,
                    "missedPercentageImpact": -10,
                    "maxScore": 100
                  }
                  ]
                }
                """);
        assertThat(configurations).hasSize(2).first().satisfies(this::verifyFirstConfiguration);
        assertThat(configurations).hasSize(2).last().satisfies(this::verifyLastConfiguration);
    }

    private void verifyFirstConfiguration(final CoverageConfiguration configuration) {
        assertThat(configuration)
                .hasCoveredPercentageImpact(1)
                .hasMissedPercentageImpact(2)
                .hasMaxScore(50)
                .isPositive()
                .hasImpact()
                .hasOnlyMetrics(
                        new CoverageParserConfiguration("JaCoCo", "jacoco.png", getMetricName(Metric.LINE)),
                        new CoverageParserConfiguration("PITest", "pit.png", getMetricName(Metric.MUTATION)));
    }

    private String getMetricName(final Metric metric) {
        return StringUtils.lowerCase(metric.name());
    }

    private void verifyLastConfiguration(final CoverageConfiguration configuration) {
        assertThat(configuration)
                .hasCoveredPercentageImpact(-5)
                .hasMissedPercentageImpact(-10)
                .hasMaxScore(100)
                .isNotPositive()
                .hasImpact()
                .hasOnlyMetrics(
                        new CoverageParserConfiguration("Cobertura", "cobertura.png", getMetricName(Metric.BRANCH)));
    }

    @ParameterizedTest(name = "{index} => Positive configuration: {1}")
    @MethodSource
    @DisplayName("should identify positive configurations")
    void shouldIdentifyPositiveValues(final String json, @SuppressWarnings("unused") final String displayName) {
        var configurations = fromJson(json);

        assertThat(configurations).hasSize(1).first().satisfies(configuration ->
                assertThat(configuration).isNotPositive().hasName(CoverageConfiguration.CODE_COVERAGE));
    }

    public static Stream<Arguments> shouldIdentifyPositiveValues() {
        return Stream.of(Arguments.of("""
                {
                  "coverage":
                    {
                    "id": "jacoco",
                    "metrics": [
                      {
                        "metric": "line"
                      }
                    ],
                    "coveredPercentageImpact": 0,
                    "missedPercentageImpact": -1,
                    "maxScore": 50
                  }
                }
                """, "missed impact is negative"),
                Arguments.of("""
                {
                  "coverage":
                    {
                    "id": "jacoco",
                    "pattern": "target/jacoco.xml",
                    "metrics": [
                      {
                        "metric": "line"
                      }
                    ],
                    "coveredPercentageImpact": -1,
                    "missedPercentageImpact": 0,
                    "maxScore": 50
                  }
                }
                """, "covered impact is negative")
                );
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CoverageConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
