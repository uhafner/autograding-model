package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.util.FilteredLog;

import static edu.hm.hafner.grading.AnalysisMarkdown.*;
import static edu.hm.hafner.grading.AnalysisScoreTest.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link AnalysisMarkdown}.
 *
 * @author Ullrich Hafner
 */
class AnalysisMarkdownTest {
    private static final String IMPACT_CONFIGURATION = "*:moneybag:*|*-1*|*-2*|*-3*|*-4*|*:ledger:*";
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var writer = new AnalysisMarkdown();

        var markdown = writer.create(new AggregatedScore("{}", LOG));

        assertThat(markdown).contains(TYPE + ": not enabled");
    }

    @Test
    void shouldShowMaximumScore() {
        var score = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "name": "Checkstyle",
                        "pattern": "target/checkstyle.xml"
                      }
                    ],
                    "errorImpact": -1,
                    "highImpact": -2,
                    "normalImpact": -3,
                    "lowImpact": -4,
                    "maxScore": 100
                  }]
                }
                """, LOG);
        score.gradeAnalysis((tool, log) -> new Report("checkstyle", "CheckStyle"));

        var markdown = new AnalysisMarkdown().create(score);

        assertThat(markdown)
                .contains("Static Analysis Warnings: 100 of 100")
                .contains("|CheckStyle|0|0|0|0|0")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var score = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "name": "Checkstyle",
                        "pattern": "target/checkstyle.xml"
                      }
                    ],
                    "name": "CheckStyle",
                    "errorImpact": -1,
                    "highImpact": -2,
                    "normalImpact": -3,
                    "lowImpact": -4,
                    "maxScore": 100
                  }]
                }
                """, LOG);
        score.gradeAnalysis((tool, log) -> createSampleReport());

        var markdown = new AnalysisMarkdown().create(score);
        assertThat(markdown)
                .contains("CheckStyle: 70 of 100")
                .contains("|CheckStyle 1|1|2|3|4|-30")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var score = new AggregatedScore("""
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
                    "name": "CheckStyle",
                    "errorImpact": -1,
                    "highImpact": -2,
                    "normalImpact": -3,
                    "lowImpact": -4,
                    "maxScore": 100
                  }]
                }
                """, LOG);
        score.gradeAnalysis((tool, log) -> createTwoReports(tool));

        var markdown = new AnalysisMarkdown().create(score);

        assertThat(markdown)
                .contains("CheckStyle: 50 of 100",
                        "|CheckStyle 1|1|2|3|4|-30",
                        "|CheckStyle 2|4|3|2|1|-20",
                        IMPACT_CONFIGURATION,
                        "**Total**|**5**|**5**|**5**|**5**|**-50**");
    }

    private static Report createSampleReport() {
        return createReportWith("CheckStyle 1",
                Severity.ERROR,
                Severity.WARNING_HIGH, Severity.WARNING_HIGH,
                Severity.WARNING_NORMAL, Severity.WARNING_NORMAL, Severity.WARNING_NORMAL,
                Severity.WARNING_LOW, Severity.WARNING_LOW, Severity.WARNING_LOW, Severity.WARNING_LOW);
    }

    private static Report createAnotherSampleReport() {
        return createReportWith("CheckStyle 2",
                Severity.ERROR, Severity.ERROR, Severity.ERROR, Severity.ERROR,
                Severity.WARNING_HIGH, Severity.WARNING_HIGH, Severity.WARNING_HIGH,
                Severity.WARNING_NORMAL, Severity.WARNING_NORMAL,
                Severity.WARNING_LOW);
    }

    static Report createTwoReports(final ToolConfiguration tool) {
        if (tool.getId().equals("checkstyle")) {
            return createSampleReport();
        }
        else if (tool.getId().equals("spotbugs")) {
            return createAnotherSampleReport();
        }
        throw new IllegalArgumentException("Unexpected tool ID: " + tool.getId());
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var score = new AggregatedScore("""
                {
                  "analysis": [
                    {
                      "name": "One",
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
                      "name": "Two",
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
                  ]
                }
                """, LOG);
        score.gradeAnalysis((tool, log) -> createTwoReports(tool));

        var markdown = new AnalysisMarkdown().create(score);

        assertThat(markdown)
                .contains("One: 30 of 100",
                        "|CheckStyle 1|1|2|3|4|30",
                        "Two: 0 of 100",
                        "|CheckStyle 2|4|3|2|1|-120",
                        "*:moneybag:*|*1*|*2*|*3*|*4*|*:ledger:*",
                        "*:moneybag:*|*-11*|*-12*|*-13*|*-14*|*:ledger:*")
                .doesNotContain("Total");
    }
}
