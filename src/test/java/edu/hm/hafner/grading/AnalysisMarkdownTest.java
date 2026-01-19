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
                .contains("|CheckStyle|Whole Project|0|0");
        assertThat(analysisMarkdown.createSummary(score))
                .contains("CheckStyle (Whole Project) - 100 of 100", "checkstyle_logo_small_64.png", "No warnings");
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
                "CS (Whole Project) - 70 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)", ":custom-icon:");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("TopLevel Warnings - 70 of 100")
                .contains("|:custom-icon:|CS|Whole Project|10|-30");
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
                "CheckStyle (Whole Project) - 70 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (Whole Project) - 80 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle - 50 of 100",
                        "|CheckStyle|Whole Project|10|-30",
                        "|SpotBugs|Whole Project|10|-20",
                        "**Total**|**-**|**20**|**-50**");
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
                "CheckStyle (Whole Project): 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (Whole Project): 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(analysisMarkdown.createDetails(score))
                .contains("CheckStyle",
                        "|CheckStyle|Whole Project|10",
                        "|SpotBugs|Whole Project|10",
                        "**Total**|**-**|**20**")
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
                        "|CheckStyle 1|Whole Project|10|30",
                        "|CheckStyle 2|Whole Project|10|30",
                        "|**Total**|**-**|**20**|**60**",
                        "Bugs - 0 of 100",
                        "|SpotBugs 1|Whole Project|10|-120",
                        "|SpotBugs 2|Whole Project|10|-120",
                        "|**Total**|**-**|**20**|**-240**");
        assertThat(analysisMarkdown.createSummary(score)).contains(
                "CheckStyle 1 (Whole Project) - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "CheckStyle 2 (Whole Project) - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs 1 (Whole Project) - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)",
                "SpotBugs 2 (Whole Project) - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
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
