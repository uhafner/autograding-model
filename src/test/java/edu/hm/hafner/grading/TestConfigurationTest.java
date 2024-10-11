package edu.hm.hafner.grading;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class TestConfigurationTest extends AbstractConfigurationTest {
    @Override
    protected List<TestConfiguration> fromJson(final String json) {
        return TestConfiguration.from(json);
    }

    @Override
    protected String getInvalidJson() {
        return """
                {
                  "tests": {
                    "name": "Unit Tests",
                  }
                }
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
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 100,
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "passedImpact": 0,
                    "failureImpact": -5,
                    "skippedImpact": -1,
                    "successRateImpact": 1,
                    "failureRateImpact": -1
                  }
                }
                """, "absolute or relative metrics", "absolute and relative values used"),
                Arguments.of("""
                {
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 0,
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "passedImpact": 0,
                    "failureImpact": 0,
                    "skippedImpact": 0,
                    "successRateImpact": 1,
                    "failureRateImpact": 0
                  }
                }
                """, "When configuring impacts then the score must not be zero.",
                        "relative impact requires positive score"),
                Arguments.of("""
                {
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 0,
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "passedImpact": 0,
                    "failureImpact": 1,
                    "skippedImpact": 0,
                    "successRateImpact": 0,
                    "failureRateImpact": 0
                  }
                }
                """, "When configuring impacts then the score must not be zero.",
                        "absolute impact requires positive score"),
                Arguments.of("""
                {
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 100,
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ]
                  }
                }
                """, "When configuring a max score than an impact must be defined as well",
                        "a score requires an impact"),
                Arguments.of("""
                {
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 100,
                    "passedImpact": 1
                  }
                }
                """, "Configuration ID 'tests' has no tools",
                        "empty tools configuration")
        );
    }

    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = fromJson("""
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
        var configurations = fromJson("""
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
        var configurations = fromJson("""
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
