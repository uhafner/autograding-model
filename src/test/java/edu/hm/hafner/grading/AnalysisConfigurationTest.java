package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class AnalysisConfigurationTest {
    @Test
    void shouldConvertObjectConfigurationFromJson() {
        var configurations = AnalysisConfiguration.from("""
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
                .hasOnlyTools(new ToolConfiguration("checkstyle", "", "target/checkstyle.xml"),
                        new ToolConfiguration("spotbugs", "", "target/spotbugsXml.xml")));
    }

    @Test
    void shouldConvertSingleArrayElementConfigurationFromJson() {
        var configurations = AnalysisConfiguration.from("""
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
        var configurations = AnalysisConfiguration.from("""
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
                      "maxScore": 5,
                      "positive": true
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
                .hasOnlyTools(new ToolConfiguration("checkstyle", "Checkstyle", "target/checkstyle.xml"),
                        new ToolConfiguration("spotbugs", "SpotBugs", "target/spotbugsXml.xml"));
    }

    private void verifyLastConfiguration(final AnalysisConfiguration configuration) {
        assertThat(configuration)
                .hasErrorImpact(-11)
                .hasHighImpact(-12)
                .hasNormalImpact(-13)
                .hasLowImpact(-14)
                .hasMaxScore(-15)
                .isNotPositive()
                .hasOnlyTools(new ToolConfiguration("pmd", "PMD", "target/pmd.xml"));
    }

    @Test
    void shouldHandleUndefinedTools() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> AnalysisConfiguration.from("""
                {
                  "analysis": {
                    "name": "Checkstyle and SpotBugs",
                    "maxScore": 50,
                    "errorImpact": 1
                  }
                }
                """)).withMessage("Configuration ID 'analysis' has no tools");
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(AnalysisConfiguration.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}
