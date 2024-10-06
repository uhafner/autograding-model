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
    private static final String IMPACT_CONFIGURATION = ":moneybag:|:heavy_minus_sign:|*-1*|*-2*|*-3*|*-4*|:heavy_minus_sign:|:heavy_minus_sign:";
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final String CHECKSTYLE = "checkstyle";
    private static final String SPOTBUGS = "spotbugs";

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var aggregation = new AggregatedScore("{}", LOG);

        var writer = new AnalysisMarkdown();

        assertThat(writer.createDetails(aggregation, true)).contains(TYPE + ": not enabled");
        assertThat(writer.createDetails(aggregation)).isEmpty();
        assertThat(writer.createSummary(aggregation)).isEmpty();
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
        score.gradeAnalysis((tool, log) -> new Report(CHECKSTYLE, "CheckStyle"));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("Static Analysis Warnings - 100 of 100")
                .contains("|CheckStyle|0|0|0|0|0|0")
                .contains(IMPACT_CONFIGURATION);
        assertThat(analysisMarkdown.createSummary(score))
                .contains("CheckStyle - 100 of 100")
                .contains("No warnings");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var score = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "pattern": "target/checkstyle.xml",
                        "name": "CS"
                      }
                    ],
                    "name": "TopLevel Warnings",
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

        assertThat(analysisMarkdown.createSummary(score)).contains(
                "CS - 70 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("TopLevel Warnings - 70 of 100")
                .contains("|CS|1|1|2|3|4|10|-30")
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
                .contains(
                        "CheckStyle - 70 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                        "SpotBugs - 80 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 50 of 100",
                        "|CheckStyle|1|1|2|3|4|10|-30",
                        "|SpotBugs|1|4|3|2|1|10|-20",
                        IMPACT_CONFIGURATION,
                        "**Total**|**2**|**5**|**5**|**5**|**5**|**20**|**-50**");
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
                .contains("CheckStyle: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                        "SpotBugs: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle",
                        "|CheckStyle|1|1|2|3|4|10",
                        "|SpotBugs|1|4|3|2|1|10",
                        "**Total**|**2**|**5**|**5**|**5**|**5**|**20**")
                .doesNotContain(IMPACT_CONFIGURATION)
                .doesNotContain("Impact");
    }

    static Report createSampleReport() {
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
        if (CHECKSTYLE.equals(tool.getId())) {
            return createSampleReport();
        }
        else if (SPOTBUGS.equals(tool.getId())) {
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
                        "|CheckStyle 1|1|1|2|3|4|10|30",
                        "|CheckStyle 2|1|1|2|3|4|10|30",
                        "|**Total**|**2**|**2**|**4**|**6**|**8**|**20**|**60**",
                        "Bugs - 0 of 100",
                        "|SpotBugs 1|1|4|3|2|1|10|-120",
                        "|SpotBugs 2|1|4|3|2|1|10|-120",
                        "|**Total**|**2**|**8**|**6**|**4**|**2**|**20**|**-240**",
                        ":moneybag:|:heavy_minus_sign:|*1*|*2*|*3*|*4*|:heavy_minus_sign:|:heavy_minus_sign:",
                        ":moneybag:|:heavy_minus_sign:|*-11*|*-12*|*-13*|*-14*|:heavy_minus_sign:|:heavy_minus_sign:");
        assertThat(analysisMarkdown.createSummary(score))
                .contains("CheckStyle 1 - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                        "CheckStyle 2 - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                        "SpotBugs 1 - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)",
                        "SpotBugs 2 - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)")
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
