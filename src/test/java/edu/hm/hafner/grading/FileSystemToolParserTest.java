package edu.hm.hafner.grading;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.FilteredLog;

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
                "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: LINE: 10.93% (33/302)",
                "-> Line Coverage Total: LINE: 10.93% (33/302)");
    }

    @Test
    void shouldCoverageCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(log);

        score.gradeCoverage(new FileSystemToolParser(), CoverageConfiguration.from(COVERAGE_CONFIGURATION));

        assertFileNodes(score.getCoveredFiles(Metric.LINE));
        assertThat(log.getInfoMessages()).contains(
                "Searching for Line Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: LINE: 10.93% (33/302)",
                "-> Line Coverage Total: LINE: 10.93% (33/302)",
                "Searching for Branch Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- src/test/resources/edu/hm/hafner/grading/jacoco.xml: BRANCH: 9.52% (4/42)",
                "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                "=> JaCoCo Score: 20 of 100",
                "Searching for Mutation Coverage results matching file name pattern **/src/**/mutations.xml",
                "- src/test/resources/edu/hm/hafner/grading/mutations.xml: MUTATION: 7.86% (11/140)",
                "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                "=> PIT Score: 16 of 100");

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

        score.gradeAnalysis(new FileSystemToolParser(), AnalysisConfiguration.from(CONFIGURATION));

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
                "- src/test/resources/edu/hm/hafner/grading/checkstyle.xml: 6 warnings",
                "-> CheckStyle (checkstyle): 6 warnings (error: 6)",
                "Searching for PMD results matching file name pattern **/src/**/pmd*.xml",
                "- src/test/resources/edu/hm/hafner/grading/pmd.xml: 4 warnings",
                "-> PMD (pmd): 4 warnings (high: 1, normal: 2, low: 1)",
                "=> Style Score: 18 of 100",
                "Searching for SpotBugs results matching file name pattern **/src/**/spotbugs*.xml",
                "- src/test/resources/edu/hm/hafner/grading/spotbugsXml.xml: 2 bugs",
                "-> SpotBugs (spotbugs): 2 bugs (low: 2)",
                "Searching for Error Prone results matching file name pattern **/src/**/error-prone.log",
                "- src/test/resources/edu/hm/hafner/grading/error-prone.log: 1 bug",
                "-> Error Prone (error-prone): 1 bug (normal: 1)",
                "=> Bugs Score: 59 of 100");

        var gradingReport = new GradingReport();
        assertThat(gradingReport.getMarkdownSummary(score)).contains(
                "Autograding score - 77 of 200 (38%)",
                "<img src=\"https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/site/resources/images/checkstyle_logo_small_64.png\"",
                "CheckStyle (project) - 6 of 100: 6 warnings (error: 6)",
                "<img src=\"https://raw.githubusercontent.com/pmd/pmd/master/docs/images/logo/PMD_small.svg\"",
                "PMD (project) - 12 of 100: 4 warnings (high: 1, normal: 2, low: 1)",
                "<img src=\"https://raw.githubusercontent.com/spotbugs/spotbugs.github.io/master/images/logos/spotbugs_icon_only_zoom_256px.png\"",
                "SpotBugs (project) - 72 of 100: 2 bugs (low: 2)",
                ":bug:",
                "Error Prone (project) - 87 of 100: 1 bug (normal: 1)");
    }
}
