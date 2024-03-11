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
    private static final String IMPACT_CONFIGURATION = ":moneybag:|*-1*|*-2*|*-3*|*-4*|:heavy_minus_sign:|:heavy_minus_sign:";
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

        assertThat(analysisMarkdown.createSummary(score)).startsWith(
                "- :warning: CheckStyle - 70 of 100: 10 warnings found (1 error, 2 high, 3 normal, 4 low)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 70 of 100")
                .contains("|CheckStyle 1|1|2|3|4|10|-30")
                .contains(IMPACT_CONFIGURATION);
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

        assertThat(analysisMarkdown.createSummary(score))
                .startsWith("- :warning: CheckStyle - 50 of 100: 20 warnings found (5 errors, 5 high, 5 normal, 5 low)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 50 of 100",
                        "|CheckStyle|1|2|3|4|10|-30",
                        "|SpotBugs|4|3|2|1|10|-20",
                        IMPACT_CONFIGURATION,
                        "**Total**|**5**|**5**|**5**|**5**|**20**|**-50**");
    }

    @Test
    void shouldShowNoImpactsWithTwoSubResults() {
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
                    "name": "CheckStyle"
                  }]
                }
                """, LOG);
        score.gradeAnalysis((tool, log) -> createTwoReports(tool));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createSummary(score))
                .startsWith("- :warning: CheckStyle: 20 warnings found (5 errors, 5 high, 5 normal, 5 low)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle",
                        "|CheckStyle|1|2|3|4|10",
                        "|SpotBugs|4|3|2|1|10",
                        "**Total**|**5**|**5**|**5**|**5**|**20**")
                .doesNotContain(IMPACT_CONFIGURATION)
                .doesNotContain("Impact");
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
                .contains("Style - 60 of 100",
                        "|CheckStyle 1|1|2|3|4|10|30",
                        "|CheckStyle 2|1|2|3|4|10|30",
                        "|**Total**|**2**|**4**|**6**|**8**|**20**|**60**",
                        "Bugs - 0 of 100",
                        "|SpotBugs 1|4|3|2|1|10|-120",
                        "|SpotBugs 2|4|3|2|1|10|-120",
                        "|**Total**|**8**|**6**|**4**|**2**|**20**|**-240**",
                        ":moneybag:|*1*|*2*|*3*|*4*|:heavy_minus_sign:|:heavy_minus_sign:",
                        ":moneybag:|*-11*|*-12*|*-13*|*-14*|:heavy_minus_sign:|:heavy_minus_sign:");
        assertThat(analysisMarkdown.createSummary(score))
                .contains("- :warning: Style - 60 of 100: 20 warnings found (2 errors, 4 high, 6 normal, 8 low)",
                        "- :warning: Bugs - 0 of 100: 20 warnings found (8 errors, 6 high, 4 normal, 2 low)")
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
                          "name": "CheckStyle 1",
                          "pattern": "target/checkstyle.xml"
                        },
                        {
                          "id": "checkstyle",
                          "name": "CheckStyle 2",
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
                          "name": "SpotBugs 1",
                          "pattern": "target/spotbugsXml.xml"
                        },
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs 2",
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
