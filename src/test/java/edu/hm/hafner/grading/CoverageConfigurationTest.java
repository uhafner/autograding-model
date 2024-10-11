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
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line",
                        "pattern": "target/jacoco.xml"
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
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line",
                        "pattern": "target/jacoco.xml"
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
                """, "Configuration ID 'coverage' has no tools",
                        "empty tools configuration")
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
                        "metric": "line",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "pit",
                        "metric": "mutation",
                        "pattern": "target/pit.xml"
                      }
                    ],
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2
                  }
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(configuration -> assertThat(configuration)
                .hasCoveredPercentageImpact(1)
                .hasMissedPercentageImpact(2)
                .hasMaxScore(50)
                .hasName("JaCoCo and PIT")
                .isPositive()
                .hasOnlyTools(new ToolConfiguration("jacoco", "", "target/jacoco.xml", "", getMetricName(Metric.LINE)),
                        new ToolConfiguration("pit", "", "target/pit.xml", "", getMetricName(Metric.MUTATION))));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "coverage": [{
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "pit",
                        "metric": "mutation",
                        "pattern": "target/pit.xml"
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
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "pit",
                        "metric": "mutation",
                        "pattern": "target/pit.xml"
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
                        "metric": "branch",
                        "pattern": "target/cobertura.xml"
                      }
                    ],
                    "metric": "branch",
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
                .hasOnlyTools(new ToolConfiguration("jacoco", "", "target/jacoco.xml", "", getMetricName(Metric.LINE)),
                        new ToolConfiguration("pit", "", "target/pit.xml", "", getMetricName(Metric.MUTATION)));
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
                .hasOnlyTools(new ToolConfiguration("cobertura", "", "target/cobertura.xml",
                        "", getMetricName(Metric.BRANCH)));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CoverageConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
