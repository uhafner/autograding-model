package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class TestConfigurationTest {
    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = TestConfiguration.from("""
                {
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 50,
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "passedImpact": 0,
                    "failureImpact": -5,
                    "skippedImpact": -1
                  }
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(configuration -> assertThat(configuration)
                .hasPassedImpact(0)
                .hasFailureImpact(-5)
                .hasSkippedImpact(-1)
                .hasMaxScore(50)
                .hasName("Unit Tests")
                .isNotPositive()
                .hasOnlyTools(new ToolConfiguration("junit", "", "target/junit.xml", StringUtils.EMPTY)));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = TestConfiguration.from("""
                {
                  "tests": [{
                    "name": "Unit Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "name": "Junit tests",
                        "pattern": "target/junit.xml"
                      },
                      {
                        "id": "jest",
                        "name": "JEST",
                        "pattern": "target/jest.xml"
                      }
                    ],
                    "maxScore": 50,
                    "passedImpact": 10,
                    "failureImpact": 5,
                    "skippedImpact": 1
                  }]
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(this::verifyFirstConfiguration);
    }

    @Test
    void shouldConvertMultipleElementsConfigurationsFromJson() {
        var configurations = TestConfiguration.from("""
                {
                  "tests": [
                  {
                    "name": "Unit Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "name": "Junit tests",
                        "pattern": "target/junit.xml"
                      },
                      {
                        "id": "jest",
                        "name": "JEST",
                        "pattern": "target/jest.xml"
                      }
                    ],
                    "maxScore": 50,
                    "passedImpact": 10,
                    "failureImpact": 5,
                    "skippedImpact": 1
                  },
                  {
                    "name": "Integration Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 500,
                    "passedImpact": 0,
                    "failureImpact": -10,
                    "skippedImpact": -1
                  }
                  ]
                }
                """);
        assertThat(configurations).hasSize(2).first().satisfies(this::verifyFirstConfiguration);
        assertThat(configurations).hasSize(2).last().satisfies(this::verifyLastConfiguration);
    }

    private void verifyFirstConfiguration(final TestConfiguration configuration) {
        assertThat(configuration)
                .hasPassedImpact(10)
                .hasFailureImpact(5)
                .hasSkippedImpact(1)
                .hasMaxScore(50)
                .isPositive()
                .hasOnlyTools(new ToolConfiguration("junit", "Junit tests", "target/junit.xml", StringUtils.EMPTY),
                        new ToolConfiguration("jest", "JEST", "target/jest.xml", StringUtils.EMPTY));
    }

    private void verifyLastConfiguration(final TestConfiguration configuration) {
        assertThat(configuration)
                .hasPassedImpact(0)
                .hasFailureImpact(-10)
                .hasSkippedImpact(-1)
                .hasMaxScore(500)
                .isNotPositive()
                .hasOnlyTools(new ToolConfiguration("junit", "", "target/junit.xml", StringUtils.EMPTY));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(TestConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
