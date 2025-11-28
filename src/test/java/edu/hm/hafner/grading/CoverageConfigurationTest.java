package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.hm.hafner.coverage.Metric;

import java.util.List;
import java.util.stream.Stream;
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
        assertThatExceptionOfType(AssertionError.class)
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
                    "tools": [
                      {
                        "pattern": "pattern",
                        "metric": "line",
                        "scope" : "project"
                      }
                    ],
                    "maxScore": 0,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2
                  }
                }
                """, "JaCoCo and PIT: When configuring impacts then the score must not be zero.",
                        "an impact requires a positive score"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "tools": [
                      {
                        "pattern": "pattern",
                        "metric": "line"
                      }
                    ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 0,
                    "missedPercentageImpact": 0
                  }
                }
                """, "JaCoCo and PIT: When configuring a score then an impact must be defined as well.",
                        "a score requires an impact"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "tools": [
                      {
                        "metric": "line",
                        "pattern": "pattern"
                      }
                    ]
                  }
                }
                """, "No tool ID specified: the ID of a tool is used to identify the parser and must not be empty.",
                        "missing ID for tool"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line"
                      }
                    ]
                  }
                }
                """, "No pattern specified: the pattern is used to select the report files to parse and must not be empty.",
                        "missing pattern for tool"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "pattern"
                      }
                    ]
                  }
                }
                """, "No metric specified: for each tool a specific coverage metric must be specified.",
                        "missing metric for tool"),
                Arguments.of("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 1
                  }
                }
                """, "JaCoCo and PIT: No tools configured.",
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
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "target/jacoco.xml",
                        "name": "JaCoCo",
                        "icon": "jacoco.png",
                        "metric": "line",
                        "scope": "project"
                      },
                      {
                        "id": "pit",
                        "pattern": "target/mutations.xml",
                        "name": "PITest",
                        "icon": "pit.png",
                        "metric": "mutation",
                        "scope": "project"
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
                .hasOnlyTools(
                        new ToolConfiguration("jacoco", "JaCoCo", "target/jacoco.xml",
                                getMetricName(Metric.LINE), "jacoco.png", "project", StringUtils.EMPTY),
                        new ToolConfiguration("pit", "PITest", "target/mutations.xml",
                                getMetricName(Metric.MUTATION), "pit.png", "project", StringUtils.EMPTY)));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "coverage": [{
                    "name": "Coverage",
                    "tools": [
                      {
                        "id": "jacoco",
                        "name": "JaCoCo",
                        "icon": "jacoco.png",
                        "pattern": "target/jacoco.xml",
                        "metric": "line",
                        "scope": "project"
                      },
                      {
                        "id": "pit",
                        "name": "PITest",
                        "icon": "pit.png",
                        "pattern": "target/mutations.xml",
                        "metric": "mutation",
                        "scope": "modified_files"
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
                    "name": "Multiple Coverages",
                    "tools": [
                      {
                        "id": "jacoco",
                        "name": "JaCoCo",
                        "pattern": "target/jacoco.xml",
                        "icon": "jacoco.png",
                        "metric": "line",
                        "scope": "project"
                      },
                      {
                        "id": "pit",
                        "name": "PITest",
                        "pattern": "target/mutations.xml",
                        "icon": "pit.png",
                        "metric": "mutation",
                        "scope": "modified_files"
                      }
                    ],
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2,
                    "maxScore": 50
                  },
                  {
                    "tools": [
                      {
                        "id": "cobertura",
                        "name": "Cobertura",
                        "pattern": "target/cobertura.xml",
                        "icon": "cobertura.png",
                        "metric": "branch",
                        "scope": "modified_lines"
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
                .hasOnlyTools(
                        new ToolConfiguration("jacoco", "JaCoCo", "target/jacoco.xml",
                                getMetricName(Metric.LINE), "jacoco.png", "project", ""),
                        new ToolConfiguration("pit", "PITest", "target/mutations.xml",
                                getMetricName(Metric.MUTATION), "pit.png", "modified_files", ""));
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
                .hasOnlyTools(
                        new ToolConfiguration("cobertura", "Cobertura", "target/cobertura.xml",
                                getMetricName(Metric.BRANCH), "cobertura.png", "modified_lines", ""));
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
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "pattern",
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
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "pattern",
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
