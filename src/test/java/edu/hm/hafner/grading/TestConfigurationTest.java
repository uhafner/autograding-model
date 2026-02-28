package edu.hm.hafner.grading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;
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

    @ParameterizedTest(name = "{index} => Invalid configuration: {0}")
    @MethodSource
    @DisplayName("should throw exceptions for invalid configurations")
    void shouldReportNotConsistentConfiguration(final String json, final String errorMessage) {
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> fromJson(json))
                .withMessageContaining(errorMessage)
                .withNoCause();
    }

    static Stream<Arguments> shouldReportNotConsistentConfiguration() {
        return Stream.of(
                Arguments.of(Named.of("an impact requires a positive score", """
                {
                  "tests": {
                    "name": "Unit Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 0,
                    "successRateImpact": 1,
                    "failureRateImpact": 0
                  }
                }
                """), "When configuring impacts then the score must not be zero."),
                Arguments.of(Named.of("a score requires an impact", """
                {
                  "tests": {
                    "name": "Unit Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 100
                  }
                }
                """), "Unit Tests: When configuring a score then an impact must be defined as well."),
                Arguments.of(Named.of("empty tools configuration", """
                {
                  "tests": {
                    "name": "Unit Tests",
                    "maxScore": 100,
                    "successRateImpact": 1
                  }
                }
                """), "Unit Tests: No tools configured.")
        );
    }

    @ParameterizedTest(name = "{index} => Negative configuration: {0}")
    @MethodSource
    @DisplayName("should identify negative configurations")
    void shouldIdentifyNegativeValues(final String json) {
        var configurations = fromJson(json);

        assertThat(configurations).hasSize(1).first().satisfies(configuration ->
                assertThat(configuration).isNotPositive());
    }

    static Stream<Arguments> shouldIdentifyNegativeValues() {
        return Stream.of(Arguments.of(Named.of("failure rate impact impact is negative", """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 0,
                    "failureRateImpact": -5
                  }
                }
                """)),
                Arguments.of(Named.of("success rate impact impact is negative", """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": -1,
                    "failureRateImpact": 0
                  }
                }
                """)),
                Arguments.of(Named.of("both impacts are negative", """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": -1,
                    "failureRateImpact": -2
                  }
                }
                """)));
    }

    @ParameterizedTest(name = "{index} => Positive configuration: {0}")
    @MethodSource
    @DisplayName("should identify positive configurations")
    void shouldIdentifyPositiveValues(final String json) {
        var configurations = fromJson(json);

        assertThat(configurations).hasSize(1).first().satisfies(configuration ->
                assertThat(configuration).isPositive());
    }

    static Stream<Arguments> shouldIdentifyPositiveValues() {
        return Stream.of(Arguments.of(Named.of("failure rate impact impact is positive", """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 0,
                    "failureRateImpact": 5
                  }
                }
                """)),
                Arguments.of(Named.of("success rate impact impact is positive", """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 1,
                    "failureRateImpact": 0
                  }
                }
                """)),
                Arguments.of(Named.of("both impacts are positive", """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 1,
                    "failureRateImpact": 2
                  }
                }
                """)));
    }

    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "tests": {
                    "name": "Unit Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 1,
                    "failureRateImpact": -5
                  }
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(configuration ->
                assertThat(configuration)
                        .hasSuccessRateImpact(1).hasFailureRateImpact(-5)
                        .hasMaxScore(50)
                        .hasName("Unit Tests")
                        .isNotPositive()
                        .hasOnlyTools(new ToolConfiguration("junit", "", "target/junit.xml", "", "", "", "")));
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
                        "pattern": "target/junit.xml",
                        "icon": "junit.png",
                        "scope": "modified_files"
                      },
                      {
                        "id": "jest",
                        "name": "JEST",
                        "pattern": "target/jest.xml",
                        "scope": "modified_files"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 1
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
                        "pattern": "target/junit.xml",
                        "name": "Junit tests",
                        "icon": "junit.png",
                        "scope": "modified_files"
                      },
                      {
                        "id": "jest",
                        "name": "JEST",
                        "pattern": "target/jest.xml",
                        "scope": "modified_files"
                      }
                    ],
                    "maxScore": 50,
                    "successRateImpact": 1
                  },
                  {
                    "name": "Integration Tests",
                    "tools": [
                      {
                        "id": "junit",
                        "pattern": "target/junit.xml",
                        "scope": "modified_lines"
                      }
                    ],
                    "maxScore": 500,
                    "successRateImpact": 0,
                    "failureRateImpact": -1
                  }
                  ]
                }
                """);
        assertThat(configurations).hasSize(2).first().satisfies(this::verifyFirstConfiguration);
        assertThat(configurations).hasSize(2).last().satisfies(this::verifyLastConfiguration);
    }

    private void verifyFirstConfiguration(final TestConfiguration configuration) {
        assertThat(configuration)
                .hasMaxScore(50)
                .hasSuccessRateImpact(1)
                .hasFailureRateImpact(0)
                .isPositive()
                .hasOnlyTools(new ToolConfiguration("junit", "Junit tests", "target/junit.xml", "", "junit.png", "modified_files", ""),
                        new ToolConfiguration("jest", "JEST", "target/jest.xml", "", "", "modified_files", ""));
    }

    private void verifyLastConfiguration(final TestConfiguration configuration) {
        assertThat(configuration)
                .hasMaxScore(500)
                .isNotPositive()
                .hasOnlyTools(new ToolConfiguration("junit", "", "target/junit.xml", "", "", "modified_lines", ""));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(TestConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
