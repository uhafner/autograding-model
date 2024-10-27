package edu.hm.hafner.grading;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.google.errorprone.annotations.MustBeClosed;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.registry.ParserRegistry;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class AggregatedScoreTest extends SerializableTest<AggregatedScore> {
    private static final String COVERAGE_CONFIGURATION = """
            {
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
    private static final String ANALYSIS_CONFIGURATION = """
            {
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "Checkstyle",
                      "pattern": "checkstyle.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "pmd.xml"
                    }
                  ],
                  "errorImpact": -1,
                  "highImpact": 2,
                  "normalImpact": 3,
                  "lowImpact": 4,
                  "maxScore": 100
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "icon": "bug",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "spotbugsXml.xml"
                    }
                  ],
                  "errorImpact": -11,
                  "highImpact": -12,
                  "normalImpact": -13,
                  "lowImpact": -14,
                  "maxScore": 100
                }
              ]
            }
            """;
    private static final String GRADING_CONFIGURATION = """
            {
              "tests": [{
                "tools": [
                  {
                    "id": "itest",
                    "name": "Integrationstests",
                    "pattern": "target/i-junit.xml"
                  },
                  {
                    "id": "mtest",
                    "name": "Modultests",
                    "pattern": "target/u-junit.xml"
                  }
                ],
                "name": "JUnit",
                "passedImpact": 10,
                "skippedImpact": -1,
                "failureImpact": -5,
                "maxScore": 100
              }],
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "Checkstyle",
                      "pattern": "target/checkstyle.xml"
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
                  "icon": "bug",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "target/spotbugsXml.xml"
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
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "target/jacoco.xml"
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
                        "pattern": "target/pit.xml"
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
                    "name": "Cyclomatic Complexity",
                    "id": "metrics",
                    "pattern": "**/src/**/metrics.xml",
                    "metric": "CyclomaticComplexity"
                  },
                  {
                    "name": "Cognitive Complexity",
                    "id": "metrics",
                    "pattern": "**/src/**/metrics.xml",
                    "metric": "CognitiveComplexity"
                  },
                  {
                    "name": "Non Commenting Source Statements",
                    "id": "metrics",
                    "pattern": "**/src/**/metrics.xml",
                    "metric": "NCSS"
                  },
                  {
                    "name": "N-Path Complexity",
                    "id": "metrics",
                    "pattern": "**/src/**/metrics.xml",
                    "metric": "NPathComplexity"
                  }
                ]
              }
            ]
            }
            """;

    private static final String QUALITY_CONFIGURATION = """
            {
              "tests": [{
                "tools": [
                  {
                    "id": "itest",
                    "name": "Integrationstests",
                    "pattern": "target/i-junit.xml"
                  },
                  {
                    "id": "mtest",
                    "name": "Modultests",
                    "pattern": "target/u-junit.xml"
                  }
                ],
                "name": "JUnit"
              }],
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "Checkstyle",
                      "pattern": "target/checkstyle.xml"
                    }
                  ]
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "icon": "bug",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "target/spotbugsXml.xml"
                    }
                  ]
                }
              ],
              "coverage": [
              {
                  "tools": [
                      {
                        "id": "jacoco",
                        "name": "Line Coverage",
                        "metric": "line",
                        "pattern": "target/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "target/jacoco.xml"
                      }
                    ],
                "name": "JaCoCo"
              },
              {
                  "tools": [
                      {
                        "id": "pit",
                        "name": "Mutation Coverage",
                        "metric": "mutation",
                        "pattern": "target/pit.xml"
                      }
                    ],
                "name": "PIT"
              }
              ]
            }
            """;

    @Override
    protected AggregatedScore createSerializable() {
        return createGradingAggregation();
    }

    static AggregatedScore createGradingAggregation() {
        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore(GRADING_CONFIGURATION, logger);

        assertThat(aggregation)
                .hasMaxScore(500)
                .hasAnalysisMaxScore(200)
                .hasTestMaxScore(100)
                .hasCoverageMaxScore(200)
                .hasCodeCoverageMaxScore(100)
                .hasMutationCoverageMaxScore(100)
                .hasAchievedScore(0)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasCodeCoverageAchievedScore(0)
                .hasMutationCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(0)
                .hasRatio(0)
                .hasTestRatio(0)
                .hasCoverageRatio(0)
                .hasCodeCoverageRatio(0)
                .hasMutationCoverageRatio(0)
                .hasAnalysisRatio(0)
                .hasToString("Score: 0 / 500")
                .hasNoAnalysisScores()
                .hasNoTestScores()
                .hasNoCoverageScores()
                .hasNoCodeCoverageScores()
                .hasNoMutationCoverageScores()
                .doesNotHaveTestFailures()
                .doesNotHaveWarnings()
                .hasAnalysis()
                .hasTests()
                .hasCoverage();

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).isEmpty();

        aggregation.gradeAnalysis((tool, log) -> AnalysisMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasAchievedScore(30)
                .hasAnalysis()
                .hasAnalysisRatio(15)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 30 / 500");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 2 static analysis configuration(s)",
                "=> Style Score: 30 of 100",
                "=> Bugs Score: 0 of 100");

        aggregation.gradeTests((tool, log) -> TestMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasAchievedScore(107)
                .hasTestAchievedScore(77)
                .hasTests()
                .hasTestRatio(77)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 107 / 500");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 test configuration(s)",
                "=> JUnit Score: 77 of 100");

        aggregation.gradeCoverage((tool, log) -> CoverageMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasAchievedScore(167)
                .hasTestAchievedScore(77)
                .hasCoverageAchievedScore(60)
                .hasCoverage()
                .hasCoverageRatio(30)
                .hasCodeCoverageAchievedScore(40)
                .hasCodeCoverage()
                .hasCodeCoverageRatio(40)
                .hasMutationCoverageAchievedScore(20)
                .hasMutationCoverage()
                .hasMutationCoverageAchievedScore(20)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 167 / 500");

        aggregation.gradeMetrics((tool, log) -> MetricMarkdownTest.createNodes(tool));

        assertThat(aggregation)
                .hasAchievedScore(167)
                .hasTestAchievedScore(77)
                .hasCoverageAchievedScore(60)
                .hasCoverage()
                .hasCoverageRatio(30)
                .hasCodeCoverageAchievedScore(40)
                .hasCodeCoverage()
                .hasCodeCoverageRatio(40)
                .hasMutationCoverageAchievedScore(20)
                .hasMutationCoverage()
                .hasMutationCoverageAchievedScore(20)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 167 / 500");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(String.join("\n", logger.getInfoMessages())).contains(
                "Processing 1 metric configuration(s)",
                "=> Cyclomatic Complexity: 10",
                "=> Cognitive Complexity: 100",
                "=> Non Commenting Source Statements: <n/a>",
                "=> N-Path Complexity: <n/a>"
        );

        assertThat(aggregation.getMetrics()).containsOnly(
                entry("cyclomatic-complexity", 10),
                entry("ncss", 0),
                entry("npath-complexity", 0),
                entry("cognitive-complexity", 100),
                entry("tests", 22),
                entry("branch", 60),
                entry("line", 80),
                entry("mutation", 60),
                entry("style", 10),
                entry("bugs", 10),
                entry("checkstyle", 10),
                entry("spotbugs", 10));
        return aggregation;
    }

    static AggregatedScore createQualityAggregation() {
        var logger = new FilteredLog("Tests");

        var aggregation = new AggregatedScore(QUALITY_CONFIGURATION, logger);
        aggregation.gradeAnalysis((tool, log) -> AnalysisMarkdownTest.createTwoReports(tool));
        aggregation.gradeTests((tool, log) -> TestMarkdownTest.createTwoReports(tool));
        aggregation.gradeCoverage((tool, log) -> CoverageMarkdownTest.createTwoReports(tool));
        return aggregation;
    }

    @Test
    void shouldHandleEmptyConfiguration() {
        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore("{}", logger);

        assertThat(aggregation)
                .hasMaxScore(0)
                .hasAnalysisMaxScore(0)
                .hasTestMaxScore(0)
                .hasCoverageMaxScore(0)
                .hasAchievedScore(0)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(0)
                .hasRatio(100)
                .hasTestRatio(100)
                .hasCoverageRatio(100)
                .hasAnalysisRatio(100)
                .hasToString("Score: 0 / 0")
                .hasNoAnalysisScores()
                .hasNoTestScores()
                .hasNoCoverageScores()
                .doesNotHaveTestFailures()
                .doesNotHaveWarnings()
                .doesNotHaveAnalysis()
                .doesNotHaveCoverage()
                .doesNotHaveTests();

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).isEmpty();
    }

    @Override
    protected void assertThatRestoredInstanceEqualsOriginalInstance(
            final AggregatedScore original, final AggregatedScore restored) {
        assertThat(restored).usingRecursiveComparison()
                .ignoringFields("log",
                        "analysisScores.report",
                        "analysisScores.subScores.report",
                        "coverageScores.report",
                        "coverageScores.subScores.report",
                        "metricScores.report",
                        "metricScores.subScores.report",
                        "testScores.report",
                        "testScores.subScores.report")
                .isEqualTo(original);
    }

    @Test
    void shouldGradeCoverageReport() {
        var aggregation = new AggregatedScore(COVERAGE_CONFIGURATION, new FilteredLog("Test"));

        aggregation.gradeCoverage((tool, log) -> readCoverageReport("jacoco.xml", tool, CoverageParserType.JACOCO));

        var coveredFiles = new String[] {"ReportFactory.java",
                "ReportFinder.java",
                "ConsoleCoverageReportFactory.java",
                "FileNameRenderer.java",
                "LogHandler.java",
                "ConsoleTestReportFactory.java",
                "AutoGradingAction.java",
                "ConsoleAnalysisReportFactory.java",
                "GitHubPullRequestWriter.java"};
        assertThat(aggregation.getCoveredFiles(Metric.LINE))
                .extracting(FileNode::getName)
                .containsExactly(coveredFiles);
        assertThat(aggregation.getCoveredFiles(Metric.BRANCH))
                .extracting(FileNode::getName)
                .containsExactly(coveredFiles);
        assertThat(aggregation.getCoveredFiles(Metric.MUTATION))
                .extracting(FileNode::getName)
                .containsExactly(coveredFiles);
        assertThat(aggregation.getIssues()).isEmpty();
    }

    @Test
    void shouldGradeAnalysisReport() {
        var aggregation = new AggregatedScore(ANALYSIS_CONFIGURATION, new FilteredLog("Test"));

        aggregation.gradeAnalysis((tool, log) -> readAnalysisReport(tool));

        assertThat(aggregation.getCoveredFiles(Metric.LINE)).isEmpty();
        assertThat(aggregation.getIssues()).extracting(Issue::getAbsolutePath).containsExactly(
                // CheckStyle:
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                "X:/Build/Results/jobs/Maven/workspace/tasks/src/main/java/hudson/plugins/tasks/parser/CsharpNamespaceDetector.java",
                // PMD:
                "C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/actions/CopyToClipboard.java",
                "C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/actions/change/ChangeSelectionAction.java",
                "C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/dialogs/SelectSourceDialog.java",
                "C:/Build/Results/jobs/ADT-Base/workspace/com.avaloq.adt.ui/src/main/java/com/avaloq/adt/env/internal/ui/dialogs/SelectSourceDialog.java",
                // SpotBugs:
                "edu/hm/hafner/analysis/IssuesTest.java",
                "edu/hm/hafner/analysis/IssuesTest.java");
    }

    static Node readCoverageReport(final String fileName, final ToolConfiguration tool,
            final CoverageParserType type) {
        var parser = new ParserRegistry().get(type, ProcessingMode.FAIL_FAST);
        try (var stream = createStream(fileName);
                var reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            var root = parser.parse(reader, fileName, new FilteredLog("Test"));
            var containerNode = new ModuleNode(tool.getDisplayName());
            containerNode.addChild(root);
            return containerNode;
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private Report readAnalysisReport(final ToolConfiguration tool) {
        try {
            var registry = new edu.hm.hafner.analysis.registry.ParserRegistry();
            return registry.get(tool.getId())
                    .createParser()
                    .parse(new FileReaderFactory(createPath(tool.getPattern())));
        }
        catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private Path createPath(final String fileName) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(AggregatedScoreTest.class.getResource(
                fileName), "File not found: " + fileName).toURI());
    }

    @MustBeClosed
    @SuppressFBWarnings("OBL")
    private static InputStream createStream(final String fileName) {
        return Objects.requireNonNull(CoverageScoreTest.class.getResourceAsStream(fileName),
                "File not found: " + fileName);
    }

    public static void main(final String... args) throws IOException {
        new AggregatedScoreTest().createSerializationFile();
    }
}
