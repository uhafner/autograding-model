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
              ]
            }
            """;
    private static final int EXPECTED_ISSUES = 6 + 4 + 2;

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
                "IssuesTest.java");
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
                "=> Bugs Score: 72 of 100");
    }
}
