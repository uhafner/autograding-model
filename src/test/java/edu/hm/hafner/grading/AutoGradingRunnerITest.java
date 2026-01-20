package edu.hm.hafner.grading;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.ResourceTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.*;

/**
 * Integration test for the grading action. Starts the container and checks if the grading runs as expected.
 *
 * @author Ullrich Hafner
 * @author Jannik Ohme
 */
class AutoGradingRunnerITest extends ResourceTest {
    private static final String CONFIGURATION = """
                  {
                    "tests": {
                      "tools": [
                        {
                          "id": "junit",
                          "name": "Unittests",
                          "pattern": "**/src/**/grading/TEST*.xml"
                        }
                      ],
                      "name": "JUnit",
                      "successRateImpact": 1,
                      "maxScore": 100
                    },
                    "analysis": [
                      {
                        "name": "Style",
                        "id": "style",
                        "tools": [
                          {
                            "id": "checkstyle",
                            "pattern": "**/src/**/checkstyle*.xml"
                          },
                          {
                            "id": "pmd",
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
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml"
                          },
                          {
                            "id": "jacoco",
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
                            "metric": "mutation",
                            "pattern": "**/src/**/mutations.xml"
                          }
                        ],
                        "name": "PIT",
                        "maxScore": 100,
                        "coveredPercentageImpact": 1,
                        "missedPercentageImpact": -1
                      }
                    ],
                    "metrics": [
                      {
                        "name": "Toplevel Metrics",
                        "tools": [
                          {
                            "id": "metrics",
                            "pattern": "**/src/**/metrics.xml",
                            "metric": "CyclomaticComplexity"
                          },
                          {
                            "id": "metrics",
                            "pattern": "**/src/**/metrics.xml",
                            "metric": "CognitiveComplexity"
                          },
                          {
                            "id": "metrics",
                            "pattern": "**/src/**/metrics.xml",
                            "metric": "NCSS"
                          },
                          {
                            "id": "metrics",
                            "pattern": "**/src/**/metrics.xml",
                            "metric": "NPathComplexity"
                          }
                        ]
                      }
                    ]
                  }
            """;
    private static final String SCOPE_CONFIGURATION = """
                  {
                    "analysis": [
                      {
                        "name": "Style",
                        "id": "style",
                        "tools": [
                          {
                            "id": "checkstyle",
                            "pattern": "**/src/**/checkstyle*.xml",
                            "scope": "modified_files"
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
                            "pattern": "**/src/**/spotbugs*.xml",
                            "scope": "modified_lines"
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
                        "name": "JaCoCo Modified Files",
                        "sourcePath": "src/main/java",
                        "tools": [
                          {
                            "id": "jacoco",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml",
                            "sourcePath": "src/main/java",
                            "scope": "modified_files"
                          }
                        ],
                        "maxScore": 100,
                        "coveredPercentageImpact": 1,
                        "missedPercentageImpact": -1
                      },
                      {
                        "name": "JaCoCo Changed Code",
                        "sourcePath": "src/main/java",
                        "tools": [
                          {
                            "id": "jacoco",
                            "metric": "branch",
                            "pattern": "**/src/**/jacoco.xml",
                            "sourcePath": "src/main/java",
                            "scope": "modified_lines"
                          }
                        ],
                        "maxScore": 100,
                        "coveredPercentageImpact": 1,
                        "missedPercentageImpact": -1
                    }
                    ]
                  }
            """;
    private static final String DELTA_CONFIGURATION = """
                  {
                    "coverage": [
                      {
                        "name": "JaCoCo",
                        "tools": [
                          {
                            "id": "jacoco",
                            "metric": "line",
                            "pattern": "**/src/**/grading/jacoco_delta.xml"
                          }
                        ],
                        "maxScore": 100,
                        "coveredPercentageImpact": 1,
                        "missedPercentageImpact": -1
                      }
                    ]
                  }
            """;
    private static final String METRICS = """
                  {
                    "metrics": {
                      "name": "Software Metrics",
                      "tools": [
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "CYCLOMATIC_COMPLEXITY"
                        },
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "COGNITIVE_COMPLEXITY"
                        },
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "NPATH_COMPLEXITY"
                        },
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "LOC"
                        },
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "NCSS"
                        },
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "COHESION"
                        },
                        {
                          "id": "metrics",
                          "pattern": "**/src/**/metrics-exception.xml",
                          "metric": "WEIGHT_OF_CLASS"
                        }
                      ]
                    }
                  }
            """;
    private static final String COVERAGE = """
                  {
                    "coverage":
                      {
                        "tools": [
                          {
                            "id": "jacoco",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco-empty-branches.xml"
                          },
                          {
                            "id": "jacoco",
                            "metric": "branch",
                            "pattern": "**/src/**/jacoco-empty-branches.xml"
                          }
                        ],
                        "name": "JaCoCo",
                        "maxScore": 100,
                        "missedPercentageImpact": -1
                      }
                  }
            """;
    private static final String TEST = """
                  {
                    "tests": {
                                 "name": "Modultests",
                                 "tools": [
                                   {
                                     "id": "junit",
                                     "name": "Modultests",
                                     "pattern": "**/src/**/test-results/TEST*.xml"
                                   }
                                 ],
                                 "failureRateImpact": -1,
                                 "maxScore": 100
                               }
                  }
            """;

    private static final String CONFIGURATION_WRONG_PATHS = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "junit",
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
                      "pattern": "**/does-not-exist/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
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
                        "metric": "line",
                        "pattern": "**/does-not-exist/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
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
    private static final String CONFIGURATION_WRONG_PATHS_CUSTOM_TOP_LEVEL_NAMES = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "junit",
                    "pattern": "**/does-not-exist/TEST*.xml"
                  }
                ],
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
                      "pattern": "**/does-not-exist/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
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
                        "metric": "line",
                        "pattern": "**/does-not-exist/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
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
    private static final String CONFIGURATION_WRONG_PATHS_CUSTOM_NAMES = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "junit",
                    "name": "tests",
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
                      "name": "checkstyle",
                      "pattern": "**/does-not-exist/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "pmd",
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
                      "name": "spotbugs",
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
                        "name": "line",
                        "metric": "line",
                        "pattern": "**/does-not-exist/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "branch",
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
                        "name": "pit",
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
        var outputStream = new ByteArrayOutputStream();
        var runner = new AutoGradingRunner(createStream(outputStream));
        var score = runner.run();

        assertThat(score.getRoundedMetrics(Scope.PROJECT)).containsOnly(
                entry("branch", "9.52"),
                entry("bugs", "2"),
                entry("checkstyle", "6"),
                entry("cognitive-complexity", "172"),
                entry("cyclomatic-complexity", "355"),
                entry("line", "10.93"),
                entry("mutation", "7.86"),
                entry("ncss", "1200"),
                entry("npath-complexity", "432"),
                entry("pmd", "4"),
                entry("spotbugs", "2"),
                entry("style", "10"),
                entry("tests", "37"),
                entry("test-success-rate", "64.86")
        );
        assertThat(score.getMetrics(Scope.PROJECT)).containsOnly(
                entry("branch", 9.52),
                entry("bugs", 2.0),
                entry("checkstyle", 6.0),
                entry("cognitive-complexity", 172.0),
                entry("cyclomatic-complexity", 355.0),
                entry("line", 10.93),
                entry("mutation", 7.86),
                entry("ncss", 1200.0),
                entry("npath-complexity", 432.0),
                entry("pmd", 4.0),
                entry("spotbugs", 2.0),
                entry("style", 10.0),
                entry("tests", 37.0),
                entry("test-success-rate", 64.86)
        );

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains("Processing 1 test configuration(s)",
                        "-> Unittests Total: 37",
                        "JUnit Score: 65 of 100",
                        "Processing 2 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 10.93% (33/302)",
                        "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                        "=> JaCoCo Score: 20 of 100",
                        "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                        "=> PIT Score: 16 of 100",
                        "Processing 2 static analysis configuration(s)",
                        "-> CheckStyle (checkstyle): 6 warnings (error: 6)",
                        "-> PMD (pmd): 4 warnings (high: 1, normal: 2, low: 1)",
                        "=> Style Score: 18 of 100",
                        "-> SpotBugs (spotbugs): 2 bugs (low: 2)",
                        "=> Bugs Score: 72 of 100",
                        "=> Cyclomatic Complexity: 355",
                        "=> Cognitive Complexity: 172",
                        "=> Non Commenting Source Statements: 1200",
                        "=> N-Path Complexity: 432",
                        "Autograding score - 191 of 500 (38%)");

        var builder = new StringCommentBuilder();
        builder.createAnnotations(score);
        assertThat(builder.getComments()).contains(
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:17-17: Die Methode 'accepts' ist nicht für Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:42-42: Zeile länger als 80 Zeichen (CheckStyle: LineLengthCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:22-22: Die Methode 'detectPackageName' ist nicht fr Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:29-29: Zeile länger als 80 Zeichen (CheckStyle: LineLengthCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:30-30: '}' sollte in derselben Zeile stehen. (CheckStyle: RightCurlyCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:37-37: '}' sollte in derselben Zeile stehen. (CheckStyle: RightCurlyCheck)",
                "[WARNING] C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/actions/CopyToClipboard.java:54-61: These nested if statements could be combined. (PMD: CollapsibleIfStatements)",
                "[WARNING] C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/actions/change/ChangeSelectionAction.java:14-14: Avoid unused imports such as 'org.eclipse.ui.IWorkbenchPart'. (PMD: UnusedImports)",
                "[WARNING] C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/dialogs/SelectSourceDialog.java:938-940: Avoid empty catch blocks. (PMD: EmptyCatchBlock)",
                "[WARNING] C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/dialogs/SelectSourceDialog.java:980-982: Avoid empty catch blocks. (PMD: EmptyCatchBlock)",
                "[WARNING] edu/hm/hafner/analysis/IssuesTest.java:286-286: Return value of Issues.get(int) ignored, but method has no side effect (SpotBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT)",
                "[WARNING] edu/hm/hafner/analysis/IssuesTest.java:289-289: Return value of Issues.get(int) ignored, but method has no side effect (SpotBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT)",
                "[NO_COVERAGE] edu/hm/hafner/grading/ReportFactory.java:15-27: Lines 15-27 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/ReportFinder.java:62-79: Lines 62-79 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/ReportFinder.java:102-103: Lines 102-103 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/ConsoleCoverageReportFactory.java:23-49: Lines 23-49 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/FileNameRenderer.java:13-15: Lines 13-15 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/LogHandler.java:19-68: Lines 19-68 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/ConsoleTestReportFactory.java:16-27: Lines 16-27 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:41-140: Lines 41-140 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:152-153: Lines 152-153 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:160-160: Line 160 is not covered by tests (Not covered line)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:164-166: Lines 164-166 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/ConsoleAnalysisReportFactory.java:17-32: Lines 17-32 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/github/GitHubPullRequestWriter.java:40-258: Lines 40-258 are not covered by tests (Not covered lines)",
                "[PARTIAL_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:146-146: Line 146 is only partially covered, one branch is missing (Partially covered line)",
                "[PARTIAL_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:159-159: Line 159 is only partially covered, one branch is missing (Partially covered line)",
                "[MUTATION_SURVIVED] edu/hm/hafner/grading/AutoGradingAction.java:147-147: One mutation survived in line 147 (VoidMethodCallMutator) (Mutation survived)",
                "[MUTATION_SURVIVED] edu/hm/hafner/grading/ReportFinder.java:29-29: One mutation survived in line 29 (EmptyObjectReturnValsMutator) (Mutation survived)");
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = SCOPE_CONFIGURATION)
    void shouldGradeScopeWithNoModified() {
        var outputStream = new ByteArrayOutputStream();
        var runner = spy(new AutoGradingRunner(createStream(outputStream)));
        when(runner.getModifiedLines(any())).thenReturn(Map.of());

        var score = runner.run();

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG",
                        "No modified lines information available",
                        "Processing 0 test configuration(s)",
                        "Processing 2 coverage configuration(s)",
                        "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: LINE: 10.93% (33/302) [Whole Project]",
                        "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: <none> [Modified Files]",
                        "-> Line Coverage Total: <none> [Modified Files]",
                        "=> JaCoCo Modified Files Score: 100 of 100 [Modified Files]",
                        "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: BRANCH: 9.52% (4/42) [Whole Project]",
                        "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: <none> [Changed Code]",
                        "-> Branch Coverage Total: <none> [Changed Code]",
                        "=> JaCoCo Changed Code Score: 100 of 100 [Changed Code]",
                        "Processing 2 static analysis configuration(s)",
                        "- src/test/resources/edu/hm/hafner/grading/checkstyle.xml: 6 warnings [Whole Project]",
                        "-> CheckStyle (checkstyle): No warnings [Modified Files]",
                        "=> Style Score: 0 of 100 [Modified Files]",
                        "- src/test/resources/edu/hm/hafner/grading/spotbugsXml.xml: 2 bugs [Whole Project]",
                        "-> SpotBugs (spotbugs): No warnings [Changed Code]",
                        "=> Bugs Score: 100 of 100 [Changed Code]",
                        "Autograding score - 300 of 400 (75%)");

        var builder = new StringCommentBuilder();
        builder.createAnnotations(score);
        assertThat(builder.getComments()).isEmpty();

        assertThat(score.getMetrics(Scope.PROJECT)).isEmpty();
        assertThat(score.getMetrics(Scope.MODIFIED_LINES)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "branch", 100.0,
                "bugs", 0.0,
                "spotbugs", 0.0)
        );
        assertThat(score.getMetrics(Scope.MODIFIED_FILES)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "line", 100.0,
                "style", 0.0,
                "checkstyle", 0.0)
        );
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = SCOPE_CONFIGURATION)
    void shouldGradeScopeWithModifiedFiles() {
        var outputStream = new ByteArrayOutputStream();
        var runner = spy(new AutoGradingRunner(createStream(outputStream)));
        when(runner.getModifiedLines(any())).thenReturn(Map.of(
                "src/main/java/edu/hm/hafner/grading/AutoGradingAction.java", Set.of(100),
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                Set.of(0),
                "src/main/java/edu/hm/hafner/analysis/IssuesTest.java", Set.of(286)));

        var score = runner.run();

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG",
                        "Modified lines information for 3 files available",
                        "- src/main/java/edu/hm/hafner/analysis/IssuesTest.java: [286]",
                        "- X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java: [0]",
                        "- src/main/java/edu/hm/hafner/grading/AutoGradingAction.java: [100]",
                        "Processing 0 test configuration(s)",
                        "Processing 2 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 10.00% (8/80) [Modified Files]",
                        "=> JaCoCo Modified Files Score: 20 of 100 [Modified Files]",
                        "-> Branch Coverage Total: <none> [Changed Code]",
                        "=> JaCoCo Changed Code Score: 100 of 100 [Changed Code]",
                        "Processing 2 static analysis configuration(s)",
                        "-> CheckStyle (checkstyle): 6 warnings (error: 6) [Modified Files]",
                        "=> Style Score: 6 of 100 [Modified Files]",
                        "-> SpotBugs (spotbugs): No warnings [Changed Code]",
                        "=> Bugs Score: 100 of 100 [Changed Code]",
                        "Autograding score - 226 of 400 (56%)");

        var builder = new StringCommentBuilder();
        builder.createAnnotations(score);
        assertThat(builder.getComments()).containsExactly(
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:17-17: Die Methode 'accepts' ist nicht für Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:42-42: Zeile länger als 80 Zeichen (CheckStyle: LineLengthCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:22-22: Die Methode 'detectPackageName' ist nicht fr Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:29-29: Zeile länger als 80 Zeichen (CheckStyle: LineLengthCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:30-30: '}' sollte in derselben Zeile stehen. (CheckStyle: RightCurlyCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:37-37: '}' sollte in derselben Zeile stehen. (CheckStyle: RightCurlyCheck)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:41-140: Lines 41-140 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:152-153: Lines 152-153 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:160-160: Line 160 is not covered by tests (Not covered line)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:164-166: Lines 164-166 are not covered by tests (Not covered lines)");

        assertThat(score.getMetrics(Scope.PROJECT)).isEmpty();
        assertThat(score.getMetrics(Scope.MODIFIED_LINES)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "branch", 100.0,
                "bugs", 0.0,
                "spotbugs", 0.0)
        );
        assertThat(score.getMetrics(Scope.MODIFIED_FILES)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "checkstyle", 6.0,
                "line", 10.0,
                "style", 6.0)
        );
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = SCOPE_CONFIGURATION)
    void shouldGradeScopeWithModifiedLines() {
        var outputStream = new ByteArrayOutputStream();
        var runner = spy(new AutoGradingRunner(createStream(outputStream)));
        when(runner.getModifiedLines(any())).thenReturn(
                Map.of("src/main/java/edu/hm/hafner/grading/AutoGradingAction.java", Set.of(42, 146),
                        "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                        Set.of(17),
                        "src/main/java/edu/hm/hafner/analysis/IssuesTest.java", Set.of(0),
                        "edu/hm/hafner/analysis/IssuesTest.java", Set.of(286)));

        var score = runner.run();

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG",
                        "Modified lines information for 4 files available",
                        "- src/main/java/edu/hm/hafner/analysis/IssuesTest.java: [0]",
                        "- src/main/java/edu/hm/hafner/grading/AutoGradingAction.java: [42, 146]",
                        "- edu/hm/hafner/analysis/IssuesTest.java: [286]",
                        "- X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java: [17]",
                        "Processing 0 test configuration(s)",
                        "Processing 2 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 10.00% (8/80) [Modified Files]",
                        "=> JaCoCo Modified Files Score: 20 of 100 [Modified Files]",
                        "-> Branch Coverage Total: BRANCH: 50.00% (1/2) [Changed Code]",
                        "=> JaCoCo Changed Code Score: 100 of 100 [Changed Code]",
                        "Processing 2 static analysis configuration(s)",
                        "-> CheckStyle (checkstyle): 6 warnings (error: 6) [Modified Files]",
                        "=> Style Score: 6 of 100 [Modified Files]",
                        "-> SpotBugs (spotbugs): 1 warning (low: 1) [Changed Code]",
                        "=> Bugs Score: 86 of 100 [Changed Code]",
                        "Autograding score - 212 of 400 (53%)");

        var builder = new StringCommentBuilder();
        builder.createAnnotations(score);
        assertThat(builder.getComments()).containsExactly(
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:17-17: Die Methode 'accepts' ist nicht für Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:42-42: Zeile länger als 80 Zeichen (CheckStyle: LineLengthCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:22-22: Die Methode 'detectPackageName' ist nicht fr Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:29-29: Zeile länger als 80 Zeichen (CheckStyle: LineLengthCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:30-30: '}' sollte in derselben Zeile stehen. (CheckStyle: RightCurlyCheck)",
                "[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:37-37: '}' sollte in derselben Zeile stehen. (CheckStyle: RightCurlyCheck)",
                "[WARNING] edu/hm/hafner/analysis/IssuesTest.java:286-286: Return value of Issues.get(int) ignored, but method has no side effect (SpotBugs: RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:41-140: Lines 41-140 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:152-153: Lines 152-153 are not covered by tests (Not covered lines)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:160-160: Line 160 is not covered by tests (Not covered line)",
                "[NO_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:164-166: Lines 164-166 are not covered by tests (Not covered lines)",
                "[PARTIAL_COVERAGE] edu/hm/hafner/grading/AutoGradingAction.java:146-146: Line 146 is only partially covered, one branch is missing (Partially covered line)");

        assertThat(score.getMetrics(Scope.PROJECT)).isEmpty();
        assertThat(score.getMetrics(Scope.MODIFIED_LINES)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "branch", 50.0,
                "bugs", 1.0,
                "spotbugs", 1.0)
        );
        assertThat(score.getMetrics(Scope.MODIFIED_FILES)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "checkstyle", 6.0,
                "line", 10.0,
                "style", 6.0)
        );
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = DELTA_CONFIGURATION)
    void shouldGradeDelta() {
        var outputStream = new ByteArrayOutputStream();
        var runner = spy(new AutoGradingRunner(createStream(outputStream)));
        when(runner.obtainDeltaReports(any())).thenReturn(Optional.of(Path.of("src/test/resources/edu/hm/hafner/grading/delta")));
        runner.run();

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains("Processing 0 test configuration(s)",
                        "Processing 1 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 100.00% (3/3) [Whole Project]",
                        "-> Line Coverage Total: LINE: 66.67% (2/3) [Whole Project]",
                        "=> JaCoCo Score: 100 of 100 [Whole Project]",
                        "Processing 0 static analysis configuration(s)",
                        "Processing 0 metric configuration(s)",
                        "Autograding score - 100 of 100");

        var c = ArgumentCaptor.forClass(AggregatedScore.class);
        verify(runner).publishGradingResult(c.capture(), any(), any());

        var report = new GradingReport();
        assertThat(report.getMarkdownDetails(c.getValue()))
                .contains("|:wavy_dash:|Line Coverage|Whole Project|100 (+33)|100");
    }

    private PrintStream createStream(final ByteArrayOutputStream outputStream) {
        return new PrintStream(outputStream, true, StandardCharsets.UTF_8);
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = COVERAGE)
    void shouldGradeOnlyCoverage() {
        var outputStream = new ByteArrayOutputStream();
        var runner = new AutoGradingRunner(createStream(outputStream));
        runner.run();
        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains("Processing 1 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 100.00% (2/2) [Whole Project]",
                        "-> Branch Coverage Total: <none> [Whole Project]",
                        "=> JaCoCo Score: 100 of 100 [Whole Project]",
                        "Autograding score - 100 of 100");
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = METRICS)
    void shouldGradeOnlyMetrics() {
        var outputStream = new ByteArrayOutputStream();
        var runner = spy(new AutoGradingRunner(createStream(outputStream)));
        runner.run();

        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains(
                        "Searching for Cyclomatic Complexity results matching file name pattern **/src/**/metrics-exception.xml",
                        "Cyclomatic Complexity Total: <none> [Whole Project]",
                        "=> Cyclomatic Complexity: <n/a> [Whole Project]",
                        "-> Cognitive Complexity Total: <none> [Whole Project]",
                        "=> Cognitive Complexity: <n/a> [Whole Project]",
                        "-> N-Path Complexity Total: <none> [Whole Project]",
                        "=> N-Path Complexity: <n/a> [Whole Project]",
                        "-> Lines of Code Total: 10 [Whole Project]",
                        "=> Lines of Code: 10 (total) [Whole Project]",
                        "-> Non Commenting Source Statements Total: 2 [Whole Project]",
                        "=> Non Commenting Source Statements: 2 (total) [Whole Project]",
                        "-> Class Cohesion Total: 0.00% [Whole Project]",
                        "=> Class Cohesion: 0.00% (maximum) [Whole Project]",
                        "-> Weight of Class Total: 0.00% [Whole Project]",
                        "=> Weight of Class: 0.00% (maximum) [Whole Project]",
                        "=> Software Metrics: <n/a> [Whole Project]");

        var c = ArgumentCaptor.forClass(AggregatedScore.class);
        verify(runner).publishGradingResult(c.capture(), any(), any());

        var report = new GradingReport();
        assertThat(report.getMarkdownDetails(c.getValue()))
                .contains("|:cyclone:|Cyclomatic Complexity|Whole Project|-|-|-|-|-",
                        "|:thought_balloon:|Cognitive Complexity|Whole Project|-|-|-|-|-",
                        "|:loop:|N-Path Complexity|Whole Project|-|-|-|-|-",
                        "|:straight_ruler:|Lines of Code|Whole Project|10|5|5|5.00|5",
                        "|:memo:|Non Commenting Source Statements|Whole Project|2|1|1|1.00|1",
                        "|:link:|Class Cohesion|Whole Project|0.00%|0.00%|0.00%|0.00%|0.00%",
                        "|:balance_scale:|Weight of Class|Whole Project|0.00%|0.00%|0.00%|0.00%|0.00%");
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = TEST)
    void shouldGradeOnlyTests() {
        var outputStream = new ByteArrayOutputStream();
        var runner = new AutoGradingRunner(createStream(outputStream));
        runner.run();
        assertThat(outputStream.toString(StandardCharsets.UTF_8))
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains("Processing 1 test configuration(s)",
                        "-> Modultests Total: 23",
                        "=> Modultests Score: 100 of 100",
                        "Autograding score - 100 of 100");
    }

    private String runAutoGrading() {
        var outputStream = new ByteArrayOutputStream();
        var runner = new AutoGradingRunner(createStream(outputStream));
        runner.run();
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION_WRONG_PATHS)
    void shouldShowErrors() {
        assertThat(runAutoGrading())
                .contains("Processing 1 test configuration(s)",
                        "Configuration error for 'Number of Tests'?",
                        "JUnit Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "=> JaCoCo Score: 100 of 100",
                        "Configuration error for 'Line Coverage'?",
                        "Configuration error for 'Branch Coverage'?",
                        "=> PIT Score: 100 of 100",
                        "Configuration error for 'Mutation Coverage'?",
                        "Processing 2 static analysis configuration(s)",
                        "Configuration error for 'CheckStyle'?",
                        "Configuration error for 'PMD'?",
                        "Configuration error for 'SpotBugs'?",
                        "-> CheckStyle (checkstyle): No warnings",
                        "-> PMD (pmd): No warnings",
                        "=> Style Score: 0 of 100",
                        "-> SpotBugs (spotbugs): No warnings",
                        "=> Bugs Score: 100 of 100",
                        "Autograding score - 400 of 500");
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION_WRONG_PATHS_CUSTOM_TOP_LEVEL_NAMES)
    void shouldShowErrorsWithCustomTopLevelNames() {
        assertThat(runAutoGrading())
                .contains("Processing 1 test configuration(s)",
                        "Configuration error for 'Number of Tests'?",
                        "Tests Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "=> JaCoCo Score: 100 of 100",
                        "Configuration error for 'Line Coverage'?",
                        "Configuration error for 'Branch Coverage'?",
                        "=> PIT Score: 100 of 100",
                        "Configuration error for 'Mutation Coverage'?",
                        "Processing 2 static analysis configuration(s)",
                        "Configuration error for 'CheckStyle'?",
                        "Configuration error for 'PMD'?",
                        "Configuration error for 'SpotBugs'?",
                        "-> CheckStyle (checkstyle): No warnings",
                        "-> PMD (pmd): No warnings",
                        "=> Style Score: 0 of 100",
                        "-> SpotBugs (spotbugs): No warnings",
                        "=> Bugs Score: 100 of 100",
                        "Autograding score - 400 of 500");
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION_WRONG_PATHS_CUSTOM_NAMES)
    void shouldShowErrorsWithCustomNames() {
        assertThat(runAutoGrading())
                .contains("Processing 1 test configuration(s)",
                        "Configuration error for 'tests'?",
                        "JUnit Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "=> JaCoCo Score: 100 of 100",
                        "Configuration error for 'line'?",
                        "Configuration error for 'branch'?",
                        "=> PIT Score: 100 of 100",
                        "Configuration error for 'pit'?",
                        "Processing 2 static analysis configuration(s)",
                        "Configuration error for 'checkstyle'?",
                        "Configuration error for 'pmd'?",
                        "Configuration error for 'spotbugs'?",
                        "-> checkstyle (checkstyle): No warnings",
                        "-> pmd (pmd): No warnings",
                        "=> Style Score: 0 of 100",
                        "-> spotbugs (spotbugs): No warnings",
                        "=> Bugs Score: 100 of 100",
                        "Autograding score - 400 of 500");
    }

    @Test
    void shouldShowNewlineBeforeErrorMessage() {
        var runner = new AutoGradingRunner();
        var log = new FilteredLog("Errors");
        log.logError("This is an error");
        assertThat(runner.createErrorMessageMarkdown(log))
                .startsWith("\n")
                .contains("## :construction: &nbsp; Error Messages")
                .contains("This is an error");
    }

    @Test
    void shouldReadDefaultConfigurationIfEnvironmentIsNotSet() {
        var runner = new AutoGradingRunner();

        var log = new FilteredLog("Errors");

        assertThat(runner.getConfiguration(log))
                .contains(toString("/default-config.json"));
        assertThat(log.getInfoMessages())
                .contains(
                        "No configuration provided (environment variable CONFIG not set), using default configuration");
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

    private static class StringCommentBuilder extends CommentBuilder {
        private final List<String> comments = new ArrayList<>();

        private List<String> getComments() {
            return comments;
        }

        @Override
        @SuppressWarnings("checkstyle:ParameterNumber")
        protected void createComment(final CommentType commentType, final String relativePath, final int lineStart,
                final int lineEnd,
                final String message, final String title,
                final int columnStart, final int columnEnd, final String details, final String markDownDetails) {
            comments.add(
                    String.format(Locale.ENGLISH, "[%s] %s:%d-%d: %s (%s)", commentType.name(), relativePath, lineStart,
                            lineEnd, message, title));
        }
    }
}
