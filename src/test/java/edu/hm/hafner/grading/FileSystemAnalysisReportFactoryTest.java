package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class FileSystemAnalysisReportFactoryTest {
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

    @Test
    void shouldCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(CONFIGURATION, log);

        score.gradeAnalysis(new FileSystemAnalysisReportFactory());

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
                "-> CheckStyle Total: 6 warnings",
                "Searching for PMD results matching file name pattern **/src/**/pmd*.xml",
                "- src/test/resources/edu/hm/hafner/grading/pmd.xml: 4 warnings",
                "-> PMD Total: 4 warnings",
                "=> Style Score: 18 of 100",
                "Searching for SpotBugs results matching file name pattern **/src/**/spotbugs*.xml",
                "- src/test/resources/edu/hm/hafner/grading/spotbugsXml.xml: 2 warnings",
                "-> SpotBugs Total: 2 warnings",
                "Searching for Error Prone results matching file name pattern **/src/**/error-prone.log",
                "- src/test/resources/edu/hm/hafner/grading/error-prone.log: 1 warnings",
                "-> Error Prone Total: 1 warnings",
                "=> Bugs Score: 59 of 100");

        var gradingReport = new GradingReport();
        assertThat(gradingReport.getMarkdownSummary(score)).contains(
                "Autograding score - 77 of 200 (38%)",
                "<img src=\"https://raw.githubusercontent.com/checkstyle/checkstyle/master/src/site/resources/images/checkstyle_logo_small_64.png\"",
                "CheckStyle - 6 of 100: 6 warnings (6 errors, 0 high, 0 normal, 0 low)",
                "<img src=\"https://raw.githubusercontent.com/pmd/pmd/master/docs/images/logo/PMD_small.svg\"",
                "PMD - 12 of 100: 4 warnings (0 error, 1 high, 2 normal, 1 low)",
                "<img src=\"https://raw.githubusercontent.com/spotbugs/spotbugs.github.io/master/images/logos/spotbugs_icon_only_zoom_256px.png\"",
                "SpotBugs - 72 of 100: 2 warnings (0 error, 0 high, 0 normal, 2 low)",
                ":bug:",
                "Error Prone - 87 of 100: 1 warning (0 error, 0 high, 1 normal, 0 low)");
    }
}
