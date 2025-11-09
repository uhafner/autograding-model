package edu.hm.hafner.grading;

import org.apache.commons.lang3.Strings;
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
    private static final String IMPACT_CONFIGURATION = ":moneybag:|:heavy_minus_sign:|:heavy_minus_sign:|:heavy_minus_sign:|*-1*|*-2*|*-3*|*-4*|:heavy_minus_sign:|:heavy_minus_sign:";
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final String CHECKSTYLE = "checkstyle";
    private static final String SPOTBUGS = "spotbugs";
    private static final String OWASP = "owasp-dependency-check";

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var aggregation = new AggregatedScore(LOG);

        var writer = new AnalysisMarkdown();

        assertThat(writer.createDetails(aggregation, true)).contains(TYPE + ": not enabled");
        assertThat(writer.createDetails(aggregation)).isEmpty();
        assertThat(writer.createSummary(aggregation)).isEmpty();
    }

    @Test
    void shouldShowMaximumScore() {
        var configuration = """
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
                """;
        var score = new AggregatedScore(LOG);
        score.gradeAnalysis(new ReportSupplier(t -> new Report(CHECKSTYLE, "CheckStyle")),
                AnalysisConfiguration.from(configuration));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("Static Analysis Warnings - 100 of 100")
                .contains("|CheckStyle|project|0|0|0|0|0|0")
                .contains(IMPACT_CONFIGURATION);
        assertThat(analysisMarkdown.createSummary(score))
                .contains("CheckStyle (project) - 100 of 100", "checkstyle_logo_small_64.png", "No warnings");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var configuration = """
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "pattern": "target/checkstyle.xml",
                        "icon": "custom-icon",
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
                """;
        var score = new AggregatedScore(LOG);
        score.gradeAnalysis(
                new ReportSupplier(t -> createSampleReport()),
                AnalysisConfiguration.from(configuration));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createSummary(score)).contains(
                "CS (project) - 70 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)", ":custom-icon:");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("TopLevel Warnings - 70 of 100")
                .contains("|:custom-icon:|CS|project|1|1|2|3|4|10|-30")
                .contains(IMPACT_CONFIGURATION);
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var configuration = """
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
                """;
        var score = new AggregatedScore(LOG);
        score.gradeAnalysis(
                new ReportSupplier(AnalysisMarkdownTest::createTwoReports),
                AnalysisConfiguration.from(configuration));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createSummary(score)).contains(
                "CheckStyle (project) - 70 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (project) - 80 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 50 of 100",
                        "|CheckStyle|project|1|1|2|3|4|10|-30",
                        "|SpotBugs|project|1|4|3|2|1|10|-20",
                        IMPACT_CONFIGURATION,
                        "**Total**|**:heavy_minus_sign:**|**2**|**5**|**5**|**5**|**5**|**20**|**-50**");
    }

    @Test
    void shouldShowNoImpactsWithTwoSubResults() {
        var configuration = """
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
                """;
        var score = new AggregatedScore(LOG);
        score.gradeAnalysis(
                new ReportSupplier(AnalysisMarkdownTest::createTwoReports),
                AnalysisConfiguration.from(configuration));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createSummary(score)).contains(
                "CheckStyle (project): 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (project): 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle",
                        "|CheckStyle|project|1|1|2|3|4|10",
                        "|SpotBugs|project|1|4|3|2|1|10",
                        "**Total**|**:heavy_minus_sign:**|**2**|**5**|**5**|**5**|**5**|**20**")
                .doesNotContain(IMPACT_CONFIGURATION)
                .doesNotContain("Impact");
    }

    @Test
    void shouldShowThreeSubResults() {
        var configuration = """
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle"
                      },
                      {
                        "id": "spotbugs"
                      },
                      {
                        "id": "owasp-dependency-check"
                      }
                    ]
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);
        score.gradeAnalysis(
                new ReportSupplier(AnalysisMarkdownTest::createTwoReports),
                AnalysisConfiguration.from(configuration));

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createSummary(score)).contains(
                "10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "10 bugs (error: 4, high: 3, normal: 2, low: 1)",
                "10 vulnerabilities (error: 4, high: 3, normal: 2, low: 1)");
    }

    static Report createSampleReport() {
        return createReportWith("checkstyle", "CheckStyle 1",
                Severity.ERROR,
                Severity.WARNING_HIGH, Severity.WARNING_HIGH,
                Severity.WARNING_NORMAL, Severity.WARNING_NORMAL, Severity.WARNING_NORMAL,
                Severity.WARNING_LOW, Severity.WARNING_LOW, Severity.WARNING_LOW, Severity.WARNING_LOW);
    }

    private static Report createAnotherSampleReport(final String id) {
        return createReportWith(id, "Other Tool " + id,
                Severity.ERROR, Severity.ERROR, Severity.ERROR, Severity.ERROR,
                Severity.WARNING_HIGH, Severity.WARNING_HIGH, Severity.WARNING_HIGH,
                Severity.WARNING_NORMAL, Severity.WARNING_NORMAL,
                Severity.WARNING_LOW);
    }

    static Report createTwoReports(final ToolConfiguration tool) {
        if (CHECKSTYLE.equals(tool.getId())) {
            return createSampleReport();
        }
        else if (Strings.CS.containsAny(tool.getId(), SPOTBUGS, OWASP)) {
            return createAnotherSampleReport(tool.getId());
        }
        throw new IllegalArgumentException("Unexpected tool ID: " + tool.getId());
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var score = createScoreForTwoResults();

        var analysisMarkdown = new AnalysisMarkdown();

        assertThat(analysisMarkdown.createDetails(score))
                .contains("Style - 60 of 100",
                        "|CheckStyle 1|project|1|1|2|3|4|10|30",
                        "|CheckStyle 2|project|1|1|2|3|4|10|30",
                        "|**Total**|**:heavy_minus_sign:**|**2**|**2**|**4**|**6**|**8**|**20**|**60**",
                        "Bugs - 0 of 100",
                        "|SpotBugs 1|project|1|4|3|2|1|10|-120",
                        "|SpotBugs 2|project|1|4|3|2|1|10|-120",
                        "|**Total**|**:heavy_minus_sign:**|**2**|**8**|**6**|**4**|**2**|**20**|**-240**",
                        ":moneybag:|:heavy_minus_sign:|:heavy_minus_sign:|:heavy_minus_sign:|*1*|*2*|*3*|*4*|:heavy_minus_sign:|:heavy_minus_sign:",
                        ":moneybag:|:heavy_minus_sign:|:heavy_minus_sign:|:heavy_minus_sign:|*-11*|*-12*|*-13*|*-14*|:heavy_minus_sign:|:heavy_minus_sign:");
        assertThat(analysisMarkdown.createSummary(score)).contains(
                "CheckStyle 1 (project) - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "CheckStyle 2 (project) - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs 1 (project) - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)",
                "SpotBugs 2 (project) - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
    }

    static AggregatedScore createScoreForTwoResults() {
        var configuration = """
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
                """;
        var score = new AggregatedScore(LOG);
        score.gradeAnalysis(
                new ReportSupplier(AnalysisMarkdownTest::createTwoReports),
                AnalysisConfiguration.from(configuration));
        return score;
    }
}
