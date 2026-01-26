package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.FilteredLog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

class FileSystemToolParserTest {
    private static final String CONFIGURATION = """
            {
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
                  "icon": "bug",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "pattern": "**/src/**/spotbugs*.xml"
                    },
                    {
                      "id": "error-prone",
                      "pattern": "**/src/**/error-prone.log"
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
    private static final int EXPECTED_ISSUES = 6 + 4 + 2 + 1;
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

    @Test
    void shouldCreateSingleReport() {
        var log = new FilteredLog("Errors");
        var jacoco = CoverageConfiguration.from("""
                {
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml"
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """
        );

        var factory = new FileSystemToolParser();

        var node = factory.readNode(jacoco.get(0).getTools().get(0), ".", log);

        assertFileNodes(node.getAllFileNodes());
        assertThat(log.getInfoMessages()).containsExactly(
                "Searching for Line Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: LINE: 10.93% (33/302) [Whole Project]",
                "-> Line Coverage Total: LINE: 10.93% (33/302) [Whole Project]");
    }

    @Test
    void shouldCoverageCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(log);

        score.gradeCoverage(new FileSystemToolParser(), CoverageConfiguration.from(COVERAGE_CONFIGURATION), Optional.empty());

        assertFileNodes(score.getCoveredFiles(Metric.LINE));
        assertThat(log.getInfoMessages()).contains(
                "Searching for Line Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: LINE: 10.93% (33/302) [Whole Project]",
                "-> Line Coverage Total: LINE: 10.93% (33/302) [Whole Project]",
                "Searching for Branch Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: BRANCH: 9.52% (4/42) [Whole Project]",
                "-> Branch Coverage Total: BRANCH: 9.52% (4/42) [Whole Project]",
                "=> JaCoCo Score: 20 of 100 [Whole Project]",
                "Searching for Mutation Coverage results matching file name pattern **/src/**/mutations.xml",
                "- src/test/resources/edu/hm/hafner/grading/mutations.xml: MUTATION: 7.86% (11/140) [Whole Project]",
                "-> Mutation Coverage Total: MUTATION: 7.86% (11/140) [Whole Project]",
                "=> PIT Score: 16 of 100 [Whole Project]");

        assertThat(score.getCoveredFiles(Metric.LINE)
                .stream()
                .map(FileNode::getMissedLineRanges)
                .flatMap(Collection::stream).collect(Collectors.toList()))
                .hasToString("[[15-27], [62-79], [102-103], [23-49], [13-15], [19-68], [16-27], "
                        + "[41-140], [152-153], [160-160], [164-166], [17-32], [40-258]]")
                .hasSize(13);
        assertThat(score.getCoveredFiles(Metric.BRANCH)
                .stream()
                .map(FileNode::getPartiallyCoveredLines)
                .filter(Predicate.not(Map::isEmpty))
                .map(Map::keySet)
                .flatMap(Collection::stream)).containsExactlyInAnyOrder(146, 159);
        assertThat(score.getCoveredFiles(Metric.MUTATION)
                .stream()
                .map(FileNode::getSurvivedMutationsPerLine)
                .filter(Predicate.not(Map::isEmpty))
                .map(Map::keySet)
                .flatMap(Collection::stream)).containsExactlyInAnyOrder(147, 29);
    }

    private void assertFileNodes(final List<FileNode> fileNodes) {
        assertThat(fileNodes).extracting(FileNode::getName).containsExactly("ReportFactory.java",
                "ReportFinder.java",
                "ConsoleCoverageReportFactory.java",
                "FileNameRenderer.java",
                "LogHandler.java",
                "ConsoleTestReportFactory.java",
                "AutoGradingAction.java",
                "ConsoleAnalysisReportFactory.java",
                "GitHubPullRequestWriter.java");
    }

    @Test
    void shouldCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(log);

        score.gradeAnalysis(new FileSystemToolParser(), AnalysisConfiguration.from(CONFIGURATION), Optional.empty());

        assertThat(score.getIssues()).hasSize(EXPECTED_ISSUES);
        assertThat(score.getIssues()).extracting(Issue::getBaseName).containsOnly(
                "CsharpNamespaceDetector.java",
                "CopyToClipboard.java",
                "ChangeSelectionAction.java",
                "SelectSourceDialog.java",
                "IssuesTest.java",
                "RobocopyParser.java");
        assertThat(score.getIssues().stream()
                .filter(issue -> "CsharpNamespaceDetector.java".equals(issue.getBaseName())))
                .map(Issue::getOriginName)
                .hasSize(6).containsOnly("CheckStyle");
        assertThat(log.getInfoMessages()).contains(
                "Searching for CheckStyle results matching file name pattern **/src/**/checkstyle*.xml",
                "- src/test/resources/edu/hm/hafner/grading/checkstyle.xml: 6 warnings [Whole Project]",
                "-> CheckStyle (checkstyle): 6 warnings (error: 6) [Whole Project]",
                "Searching for PMD results matching file name pattern **/src/**/pmd*.xml",
                "- src/test/resources/edu/hm/hafner/grading/pmd.xml: 4 warnings [Whole Project]",
                "-> PMD (pmd): 4 warnings (high: 1, normal: 2, low: 1) [Whole Project]",
                "=> Style Score: 18 of 100 [Whole Project]",
                "Searching for SpotBugs results matching file name pattern **/src/**/spotbugs*.xml",
                "- src/test/resources/edu/hm/hafner/grading/spotbugsXml.xml: 2 bugs [Whole Project]",
                "-> SpotBugs (spotbugs): 2 bugs (low: 2) [Whole Project]",
                "Searching for Error Prone results matching file name pattern **/src/**/error-prone.log",
                "- src/test/resources/edu/hm/hafner/grading/error-prone.log: 1 bug [Whole Project]",
                "-> Error Prone (error-prone): 1 bug (normal: 1) [Whole Project]",
                "=> Bugs Score: 59 of 100 [Whole Project]");

        var gradingReport = new GradingReport();
        assertThat(gradingReport.getMarkdownSummary(score)).contains(
                "Autograding score - 77 of 200 (38%)",
                "<img src=\"https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/site/resources/images/checkstyle_logo_small_64.png\"",
                "CheckStyle (Whole Project) - 6 of 100: 6 warnings (error: 6)",
                "<img src=\"https://raw.githubusercontent.com/pmd/pmd/master/docs/images/logo/PMD_small.svg\"",
                "PMD (Whole Project) - 12 of 100: 4 warnings (high: 1, normal: 2, low: 1)",
                "<img src=\"https://raw.githubusercontent.com/spotbugs/spotbugs.github.io/master/images/logos/spotbugs_icon_only_zoom_256px.png\"",
                "SpotBugs (Whole Project) - 72 of 100: 2 bugs (low: 2)",
                ":bug:",
                "Error Prone (Whole Project) - 87 of 100: 1 bug (normal: 1)");
    }

    @Test
    void shouldFilterNodesByModifiedLinesInSingleModuleProject() {
        var log = new FilteredLog("Errors");

        // Simulate modified lines from GitHub PR diff
        var modifiedLines = Map.of(
                "src/main/java/edu/hm/hafner/grading/AutoGradingAction.java", Set.of(42, 146, 160),
                "src/main/java/edu/hm/hafner/grading/ReportFinder.java", Set.of(29, 58)
        );

        var parser = new FileSystemToolParser(modifiedLines);
        var jacoco = CoverageConfiguration.from("""
                {
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml"
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """
        );

        var node = parser.readNode(jacoco.get(0).getTools().get(0), ".", log);

        // Verify that modified lines were assigned to matching files
        var autoGradingAction = node.getAllFileNodes().stream()
                .filter(f -> f.getName().equals("AutoGradingAction.java"))
                .findFirst()
                .orElseThrow();

        assertThat(autoGradingAction.hasModifiedLines()).isTrue();
        assertThat(autoGradingAction.getModifiedLines()).containsExactlyInAnyOrder(42, 146, 160);

        var reportFinder = node.getAllFileNodes().stream()
                .filter(f -> f.getName().equals("ReportFinder.java"))
                .findFirst()
                .orElseThrow();

        assertThat(reportFinder.hasModifiedLines()).isTrue();
        assertThat(reportFinder.getModifiedLines()).containsExactlyInAnyOrder(29, 58);

        // Verify logging
        assertThat(log.getInfoMessages())
                .anyMatch(msg -> msg.contains("Matched coverage file"))
                .anyMatch(msg -> msg.contains("Successfully matched"));
    }

    @Test
    void shouldFilterNodesByModifiedLinesWithDifferentPathFormats() {
        var log = new FilteredLog("Errors");

        // Test various path formats that should all match
        var modifiedLines = Map.of(
                // Full repository path (most common in multi-module projects)
                "src/main/java/edu/hm/hafner/grading/ReportFactory.java", Set.of(15, 17),
                // Partial path without source prefix
                "edu/hm/hafner/grading/AutoGradingAction.java", Set.of(145, 146)
        );

        var parser = new FileSystemToolParser(modifiedLines);
        var jacoco = CoverageConfiguration.from("""
                {
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml"
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """
        );

        var node = parser.readNode(jacoco.get(0).getTools().get(0), ".", log);

        // Verify that files with different path formats were matched
        var reportFactory = node.getAllFileNodes().stream()
                .filter(f -> f.getName().equals("ReportFactory.java"))
                .findFirst()
                .orElseThrow();

        assertThat(reportFactory.hasModifiedLines()).isTrue();
        assertThat(reportFactory.getModifiedLines()).containsExactlyInAnyOrder(15, 17);

        var autoGradingAction = node.getAllFileNodes().stream()
                .filter(f -> f.getName().equals("AutoGradingAction.java"))
                .findFirst()
                .orElseThrow();

        assertThat(autoGradingAction.hasModifiedLines()).isTrue();
        assertThat(autoGradingAction.getModifiedLines()).containsExactlyInAnyOrder(145, 146);

        // Verify successful matching was logged
        assertThat(log.getInfoMessages())
                .anyMatch(msg -> msg.contains("Successfully matched 2 coverage files"));
    }

    @Test
    void shouldHandleNoMatchesWhenModifiedLinesDoNotMatchCoverageFiles() {
        var log = new FilteredLog("Errors");

        // Provide modified lines for files that don't exist in the coverage report
        var modifiedLines = Map.of(
                "app/src/main/java/com/example/NonExistent.java", Set.of(10, 20),
                "module-a/src/main/java/com/example/Another.java", Set.of(5)
        );

        var parser = new FileSystemToolParser(modifiedLines);
        var jacoco = CoverageConfiguration.from("""
                {
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml"
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """
        );

        var node = parser.readNode(jacoco.get(0).getTools().get(0), ".", log);

        // Verify that no files have modified lines assigned
        assertThat(node.getAllFileNodes())
                .noneMatch(FileNode::hasModifiedLines);

        // Verify warning was logged
        assertThat(log.getInfoMessages())
                .anyMatch(msg -> msg.contains("Warning: No coverage files matched to PR diff files"));
    }

    @Test
    void shouldHandleEmptyModifiedLinesMap() {
        var log = new FilteredLog("Errors");

        // Empty modified lines map (no PR changes)
        var parser = new FileSystemToolParser(Map.of());
        var jacoco = CoverageConfiguration.from("""
                {
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml"
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """
        );

        var node = parser.readNode(jacoco.get(0).getTools().get(0), ".", log);

        // Verify that no files have modified lines (but parsing still works)
        assertThat(node.getAllFileNodes())
                .noneMatch(FileNode::hasModifiedLines)
                .isNotEmpty(); // Files should still be present

        // Verify no matching warnings in log
        assertThat(log.getInfoMessages())
                .noneMatch(msg -> msg.contains("Matched coverage file"))
                .noneMatch(msg -> msg.contains("Warning: No coverage files matched"));
    }

    @Test
    void shouldHandleBidirectionalSuffixMatching() {
        var log = new FilteredLog("Errors");

        // Test bidirectional suffix matching: PR diff has shorter path, coverage has longer
        var modifiedLines = Map.of(
                // Shorter path from diff
                "edu/hm/hafner/grading/ReportFinder.java", Set.of(29, 36, 40)
        );

        var parser = new FileSystemToolParser(modifiedLines);
        var jacoco = CoverageConfiguration.from("""
                {
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "**/src/**/jacoco.xml",
                            "sourcePath": ""
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """
        );

        var node = parser.readNode(jacoco.get(0).getTools().get(0), ".", log);

        // Coverage report has "edu/hm/hafner/grading/ReportFinder.java"
        // Should match with "edu/hm/hafner/grading/ReportFinder.java" from diff
        var reportFinder = node.getAllFileNodes().stream()
                .filter(f -> f.getName().equals("ReportFinder.java"))
                .findFirst()
                .orElseThrow();

        assertThat(reportFinder.hasModifiedLines()).isTrue();
        assertThat(reportFinder.getModifiedLines()).containsExactlyInAnyOrder(29, 36, 40);
    }
}
