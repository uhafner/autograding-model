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

class AnalysisConfigurationTest extends AbstractConfigurationTest {
    @Override
    protected List<AnalysisConfiguration> fromJson(final String json) {
        return AnalysisConfiguration.from(json);
    }

    @Override
    protected String getInvalidJson() {
        return """
                {
                  "analysis": {
                    "name": "Checkstyle and SpotBugs",
                    "maxScore": 50,
                    "errorImpact": 1
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
                  "analysis": [
                    {
                      "tools": [
                        {
                          "id": "checkstyle",
                          "name": "Checkstyle"
                        }
                      ],
                      "errorImpact": 1,
                      "highImpact": 2,
                      "normalImpact": 3,
                      "lowImpact": 4,
                      "maxScore": 0
                    }
                  ]
                }
                """, "When configuring impacts then the score must not be zero.",
                        "an impact requires a positive score"),
                Arguments.of("""
                {
                  "analysis": [
                    {
                      "tools": [
                        {
                          "id": "checkstyle",
                          "name": "Checkstyle"
                        }
                      ],
                      "errorImpact": 0,
                      "highImpact": 0,
                      "normalImpact": 0,
                      "lowImpact": 0,
                      "maxScore": 100
                    }
                  ]
                }
                """, "When configuring a max score than an impact must be defined as well",
                        "a score requires an impact"),
                Arguments.of("""
                {
                  "analysis": [
                    {
                      "errorImpact": 1,
                      "highImpact": 2,
                      "normalImpact": 3,
                      "lowImpact": 4,
                      "maxScore": 20
                    }
                  ]
                }
                """, "Configuration ID 'analysis' has no tools",
                        "empty tools configuration")
        );
    }

    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "analysis": {
                    "name": "Checkstyle and SpotBugs",
                    "maxScore": 50,
                    "tools": [
                      {
                        "id": "checkstyle",
                        "pattern": "target/checkstyle.xml"
                      },
                      {
                        "id": "spotbugs",
                        "pattern": "target/spotbugsXml.xml"
                      }
                    ],
                    "errorImpact": 1
                  }
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(configuration -> assertThat(configuration)
                .hasErrorImpact(1)
                .hasHighImpact(0)
                .hasNormalImpact(0)
                .hasLowImpact(0)
                .hasMaxScore(50)
                .hasName("Checkstyle and SpotBugs")
                .isPositive()
                .hasOnlyTools(new ToolConfiguration("checkstyle", "", "target/checkstyle.xml", StringUtils.EMPTY),
                        new ToolConfiguration("spotbugs", "", "target/spotbugsXml.xml", StringUtils.EMPTY)));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = fromJson("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "name": "Checkstyle",
                        "pattern": "target/checkstyle.xml"
                      },
                      {
                        "id": "spotbugs",
                        "name": "SpotBugs",
                        "pattern": "target/spotbugsXml.xml"
                      }
                    ],
                    "errorImpact": 1,
                    "highImpact": 2,
                    "normalImpact": 3,
                    "lowImpact": 4,
                    "maxScore": 5
                  }]
                }
                """);

        assertThat(configurations).hasSize(1).first().satisfies(this::verifyFirstConfiguration);
    }

    @Test
    void shouldConvertMultipleElementsConfigurationsFromJson() {
        var configurations = fromJson("""
                {
                  "analysis": [
                    {
                      "tools": [
                        {
                          "id": "checkstyle",
                          "name": "Checkstyle",
                          "pattern": "target/checkstyle.xml"
                        },
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                      "errorImpact": 1,
                      "highImpact": 2,
                      "normalImpact": 3,
                      "lowImpact": 4,
                      "maxScore": 5
                    },
                    {
                      "tools": [
                        {
                          "id": "pmd",
                          "name": "PMD",
                          "pattern": "target/pmd.xml"
                        }
                      ],
                      "errorImpact": -11,
                      "highImpact": -12,
                      "normalImpact": -13,
                      "lowImpact": -14,
                      "maxScore": -15
                    }
                  ]
                }
                """);
        assertThat(configurations).hasSize(2).first().satisfies(this::verifyFirstConfiguration);
        assertThat(configurations).hasSize(2).last().satisfies(this::verifyLastConfiguration);
    }

    private void verifyFirstConfiguration(final AnalysisConfiguration configuration) {
        assertThat(configuration)
                .hasErrorImpact(1)
                .hasHighImpact(2)
                .hasNormalImpact(3)
                .hasLowImpact(4)
                .hasMaxScore(5)
                .isPositive()
                .hasOnlyTools(new ToolConfiguration("checkstyle", "Checkstyle", "target/checkstyle.xml",
                                StringUtils.EMPTY),
                        new ToolConfiguration("spotbugs", "SpotBugs", "target/spotbugsXml.xml", StringUtils.EMPTY));
    }

    private void verifyLastConfiguration(final AnalysisConfiguration configuration) {
        assertThat(configuration)
                .hasErrorImpact(-11)
                .hasHighImpact(-12)
                .hasNormalImpact(-13)
                .hasLowImpact(-14)
                .hasMaxScore(-15)
                .isNotPositive()
                .hasOnlyTools(new ToolConfiguration("pmd", "PMD", "target/pmd.xml", StringUtils.EMPTY));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(AnalysisConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
