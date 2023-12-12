package edu.hm.hafner.grading;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.ResourceTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the grading action. Starts the container and checks if the grading runs as expected.
 *
 * @author Ullrich Hafner
 */
public class AutoGradingRunnerITest extends ResourceTest {
    private static final String CONFIGURATION = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "test",
                    "name": "Unittests",
                    "pattern": "**/src/**/TEST*.xml"
                  }
                ],
                "name": "JUnit",
                "passedImpact": 10,
                "skippedImpact": -1,
                "failureImpact": -5,
                "maxScore": 100
              },
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "CheckStyle",
                      "pattern": "**/src/**/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "**/src/**/pmd*.xml"
                    }
                  ],
                  "errorImpact": 1,
                  "highImpact": 2,
                  "normalImpact": 3,
                  "lowImpact": 4,
                  "maxScore": 100
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "**/src/**/spotbugs*.xml"
                    }
                  ],
                  "errorImpact": -11,
                  "highImpact": -12,
                  "normalImpact": -13,
                  "lowImpact": -14,
                  "maxScore": 100
                }
              ],
              "coverage": [
              {
                  "tools": [
                      {
                        "id": "jacoco",
                        "name": "Line Coverage",
                        "metric": "line",
                        "pattern": "**/src/**/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "**/src/**/jacoco.xml"
                      }
                    ],
                "name": "JaCoCo",
                "maxScore": 100,
                "coveredPercentageImpact": 1,
                "missedPercentageImpact": -1
              },
              {
                  "tools": [
                      {
                        "id": "pit",
                        "name": "Mutation Coverage",
                        "metric": "mutation",
                        "pattern": "**/src/**/mutations.xml"
                      }
                    ],
                "name": "PIT",
                "maxScore": 100,
                "coveredPercentageImpact": 1,
                "missedPercentageImpact": -1
              }
              ]
            }
            """;
    private static final String CONFIGURATION_WRONG_PATHS = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "test",
                    "name": "Unittests",
                    "pattern": "**/does-not-exist/TEST*.xml"
                  }
                ],
                "name": "JUnit",
                "passedImpact": 10,
                "skippedImpact": -1,
                "failureImpact": -5,
                "maxScore": 100
              },
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "CheckStyle",
                      "pattern": "**/does-not-exist/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "**/does-not-exist/pmd*.xml"
                    }
                  ],
                  "errorImpact": 1,
                  "highImpact": 2,
                  "normalImpact": 3,
                  "lowImpact": 4,
                  "maxScore": 100
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "**/does-not-exist/spotbugs*.xml"
                    }
                  ],
                  "errorImpact": -11,
                  "highImpact": -12,
                  "normalImpact": -13,
                  "lowImpact": -14,
                  "maxScore": 100
                }
              ],
              "coverage": [
              {
                  "tools": [
                      {
                        "id": "jacoco",
                        "name": "Line Coverage",
                        "metric": "line",
                        "pattern": "**/does-not-exist/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "**/does-not-exist/jacoco.xml"
                      }
                    ],
                "name": "JaCoCo",
                "maxScore": 100,
                "coveredPercentageImpact": 1,
                "missedPercentageImpact": -1
              },
              {
                  "tools": [
                      {
                        "id": "pit",
                        "name": "Mutation Coverage",
                        "metric": "mutation",
                        "pattern": "**/does-not-exist/mutations.xml"
                      }
                    ],
                "name": "PIT",
                "maxScore": 100,
                "coveredPercentageImpact": 1,
                "missedPercentageImpact": -1
              }
              ]
            }
            """;

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION)
    void shouldGradeWithConfigurationFromEnvironment() {
        assertThat(runAutoGrading())
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains(new String[] {
                        "Processing 1 test configuration(s)",
                        "-> Unittests Total: TESTS: 37 tests",
                        "JUnit Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 10.93% (33/302)",
                        "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                        "=> JaCoCo Score: 20 of 100",
                        "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                        "=> PIT Score: 16 of 100",
                        "Processing 2 static analysis configuration(s)",
                        "-> CheckStyle Total: 6 warnings",
                        "-> PMD Total: 4 warnings",
                        "=> Style Score: 18 of 100",
                        "-> SpotBugs Total: 2 warnings",
                        "=> Bugs Score: 72 of 100",
                        "Total score - 226 of 500 (unit tests: 100/100, code coverage: 20/100, mutation coverage: 16/100, analysis: 90/200)"});
    }

    private String runAutoGrading() {
        var outputStream = new ByteArrayOutputStream();
        var runner = new AutoGradingRunner(new PrintStream(outputStream));
        runner.run();
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Test @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION_WRONG_PATHS)
    void shouldShowErrors() {
        assertThat(runAutoGrading())
                .contains(new String[] {
                        "Processing 1 test configuration(s)",
                        "-> Unittests Total: TESTS: 0 tests",
                        "Configuration error for 'Unittests'?",
                        "JUnit Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "=> JaCoCo Score: 0 of 100",
                        "Configuration error for 'Line Coverage'?",
                        "Configuration error for 'Branch Coverage'?",
                        "=> PIT Score: 0 of 100",
                        "Configuration error for 'Mutation Coverage'?",
                        "Processing 2 static analysis configuration(s)",
                        "Configuration error for 'CheckStyle'?",
                        "Configuration error for 'PMD'?",
                        "Configuration error for 'SpotBugs'?",
                        "-> CheckStyle Total: 0 warnings",
                        "-> PMD Total: 0 warnings",
                        "=> Style Score: 0 of 100",
                        "-> SpotBugs Total: 0 warnings",
                        "=> Bugs Score: 100 of 100",
                        "Total score - 200 of 500 (unit tests: 100/100, code coverage: 0/100, mutation coverage: 0/100, analysis: 100/200)"});
    }

    @Test
    void shouldReadDefaultConfigurationIfEnvironmentIsNotSet() {
        var runner = new AutoGradingRunner();

        var log = new FilteredLog("Errors");

        assertThat(runner.getConfiguration(log))
                .contains(toString("/default-config.json"));
        assertThat(log.getInfoMessages())
                .contains("No configuration provided (environment variable CONFIG not set), using default configuration");
        assertThat(log.getErrorMessages()).isEmpty();
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = "{}")
    void shouldReadConfigurationFromEnvironment() {
        var runner = new AutoGradingRunner();

        var log = new FilteredLog("Errors");

        assertThat(runner.getConfiguration(log)).isEqualTo("{}");
        assertThat(log.getInfoMessages()).contains("Obtaining configuration from environment variable CONFIG");
        assertThat(log.getErrorMessages()).isEmpty();
    }
}
