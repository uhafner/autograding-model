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
    private static final String IMPACT_CONFIGURATION = "*:moneybag:*|*-1*|*-2*|*-3*|*-4*|*:heavy_plus_sign:*|*:ledger:*";
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var aggregation = new AggregatedScore("{}", LOG);

        var writer = new AnalysisMarkdown();

        assertThat(writer.createDetails(aggregation)).contains(TYPE + ": not enabled");
        assertThat(writer.createSummary(aggregation)).contains(TYPE + ": not enabled");
    }

    @Test
    void shouldShowMaximumScore() {
        var score = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
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

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("Static Analysis Warnings - 100 of 100")
                .contains("|CheckStyle|0|0|0|0|0|0")
                .contains(IMPACT_CONFIGURATION);
        assertThat(analysisMarkdown.createSummary(score))
                .contains("Static Analysis Warnings - 100 of 100")
                .contains("No warnings found");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var score = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
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

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 70 of 100")
                .contains("|CheckStyle 1|1|2|3|4|10|-30")
                .contains(IMPACT_CONFIGURATION);
        assertThat(analysisMarkdown.createSummary(score))
                .contains("CheckStyle - 70 of 100")
                .contains("10 warnings found (1 errors, 2 high, 3 normal, 4 low)");
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var score = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "name": "CheckStyle",
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

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 50 of 100",
                        "|CheckStyle|1|2|3|4|10|-30",
                        "|SpotBugs|4|3|2|1|10|-20",
                        IMPACT_CONFIGURATION,
                        "**Total**|**5**|**5**|**5**|**5**|**20**|**-50**");
        assertThat(analysisMarkdown.createSummary(score))
                .contains("CheckStyle - 50 of 100",
                        "20 warnings found (5 errors, 5 high, 5 normal, 5 low)");
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
        var score = createScoreForTwoResults();

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("Style - 30 of 100",
                        "|CheckStyle|1|2|3|4|10|30",
                        "Bugs - 0 of 100",
                        "|SpotBugs|4|3|2|1|10|-120",
                        "*:moneybag:*|*1*|*2*|*3*|*4*|*:heavy_plus_sign:*|*:ledger:*",
                        "*:moneybag:*|*-11*|*-12*|*-13*|*-14*|*:heavy_plus_sign:*|*:ledger:*");
        assertThat(analysisMarkdown.createSummary(score))
                .contains("Style - 30 of 100",
                        "10 warnings found (1 errors, 2 high, 3 normal, 4 low)",
                        "Bugs - 0 of 100",
                        "10 warnings found (4 errors, 3 high, 2 normal, 1 low)")
                .doesNotContain("Total");
    }

    static AggregatedScore createScoreForTwoResults() {
        var score = new AggregatedScore("""
                {
                  "analysis": [
                    {
                      "name": "Style",
                      "tools": [
                        {
                          "id": "checkstyle",
                          "name": "CheckStyle",
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
        return score;
    }
}
