package edu.hm.hafner.grading;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
        var outputStream = new ByteArrayOutputStream();
        var runner = new AutoGradingRunner(new PrintStream(outputStream));
        var score = runner.run();
        assertThat(outputStream.toString(StandardCharsets.UTF_8))
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
                        "Autograding score - 226 of 500"});

        var builder = new StringCommentBuilder();
        builder.createAnnotations(score);
        assertThat(builder.getComments()).contains("[WARNING] X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java:17-17: Die Methode 'accepts' ist nicht für Vererbung entworfen - muss abstract, final oder leer sein. (CheckStyle: DesignForExtensionCheck)",
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
                        "Autograding score - 200 of 500"});
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

    private static class StringCommentBuilder extends CommentBuilder {
        private final List<String> comments = new ArrayList<>();

        public List<String> getComments() {
            return comments;
        }

        @Override @SuppressWarnings("checkstyle:ParameterNumber")
        protected void createComment(final CommentType commentType, final String relativePath, final int lineStart, final int lineEnd,
                final String message, final String title,
                final int columnStart, final int columnEnd, final String details, final String markDownDetails) {
            comments.add(String.format("[%s] %s:%d-%d: %s (%s)", commentType.name(), relativePath, lineStart, lineEnd, message, title));
        }
    }
}
