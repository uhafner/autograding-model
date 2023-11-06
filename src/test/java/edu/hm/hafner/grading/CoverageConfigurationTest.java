package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class CoverageConfigurationTest {
    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = CoverageConfiguration.from("""
                {
                  "coverage": {
                    "name": "JaCoCo and PIT",
                    "maxScore": 50,
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "pit",
                        "pattern": "target/pit.xml"
                      }
                    ],
                    "metric": "line",
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2
                  }
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(configuration -> assertThat(configuration)
                .hasCoveredPercentageImpact(1)
                .hasMissedPercentageImpact(2)
                .hasMetric(Metric.LINE)
                .hasMaxScore(50)
                .hasName("JaCoCo and PIT")
                .isPositive()
                .hasOnlyTools(new ToolConfiguration("jacoco", "", "target/jacoco.xml"),
                        new ToolConfiguration("pit", "", "target/pit.xml")));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = CoverageConfiguration.from("""
                {
                  "coverage": [{
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "pit",
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
        var configurations = CoverageConfiguration.from("""
                {
                  "coverage": [
                    {
                    "tools": [
                      {
                        "id": "jacoco",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "pit",
                        "pattern": "target/pit.xml"
                      }
                    ],
                    "metric": "line",
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": 2,
                    "maxScore": 50
                  },
                  {
                    "tools": [
                      {
                        "id": "cobertura",
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
                .hasOnlyTools(new ToolConfiguration("jacoco", "", "target/jacoco.xml"),
                        new ToolConfiguration("pit", "", "target/pit.xml"));
    }

    private void verifyLastConfiguration(final CoverageConfiguration configuration) {
        assertThat(configuration)
                .hasCoveredPercentageImpact(-5)
                .hasMissedPercentageImpact(-10)
                .hasMaxScore(100)
                .isNotPositive()
                .hasOnlyTools(new ToolConfiguration("cobertura", "", "target/cobertura.xml"));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CoverageConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
