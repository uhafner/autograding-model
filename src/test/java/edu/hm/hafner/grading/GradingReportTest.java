package edu.hm.hafner.grading;

import edu.hm.hafner.util.FilteredLog;
import org.junitpioneer.jupiter.DefaultLocale;

import java.util.NoSuchElementException;

import static edu.hm.hafner.grading.assertions.Assertions.assertThat;
import static edu.hm.hafner.grading.assertions.Assertions.entry;

/**
 * Tests the class {@link GradingReport}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class GradingReportTest {
    private static final String NO_SCORE_CONFIGURATION = """
            {
              "tests": [{
                "name": "JUnit",
                "tools": [
                  {
                    "name": "Integrationstests",
                    "id": "junit",
                    "pattern": "**/TEST-*.xml"
                  },
                  {
                    "name": "Modultests",
                    "id": "junit",
                    "pattern": "**/TEST-*.xml"
                  }
                ]
              }],
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                        "name": "Checkstyle",
                        "id": "checkstyle"
                    }
                  ]
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "icon": "bug",
                  "tools": [
                    {
                        "name": "SpotBugs",
                        "id": "spotbugs"
                    }
                  ]
                }
              ],
              "coverage": [
              {
                "name": "JaCoCo",
                "tools": [
                  {
                    "id": "jacoco",
                    "name": "Line Coverage",
                    "pattern": "**/jacoco.xml",
                    "metric": "line"
                  },
                  {
                    "id": "jacoco",
                    "name": "Branch Coverage",
                    "pattern": "**/jacoco.xml",
                    "metric": "branch"
                  }
                ]
              },
              {
                "name": "PIT",
                "tools" : [
                  {
                    "id": "pit",
                    "name": "Mutation Coverage",
                    "pattern": "**/mutations.xml",
                    "metric": "mutation"
                  }
                ]
              }
              ]
            }
            """;

    void shouldCreateEmptyResults() {
        var results = new GradingReport();

        var score = new AggregatedScore();
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score");
        var disabledScores = new String[]{
                "Test Score: not enabled",
                "Metrics Score: not enabled",
                "Code Coverage Score: not enabled",
                "Mutation Coverage Score: not enabled",
                "Static Analysis Score: not enabled"};
        assertThat(results.getMarkdownDetails(score, "Title", true, false))
                .contains("Title")
                .contains(disabledScores);
        assertThat(results.getMarkdownDetails(score, "Title"))
                .contains("Title")
                .doesNotContain(disabledScores);
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .contains("Summary");
    }

    void shouldCreateAllGradingResults() {
        var results = new GradingReport();

        var score = new AggregatedScoreTest().createSerializable();
        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "# :mortar_board: &nbsp; Summary - 167 of 500",
                "Integrationstests (project) - 27 of 100: 55.56% successful",
                "Modultests (project) - 50 of 100: 0.00% successful",
                "Line Coverage (project) - 60 of 100", "80.00% (20 missed lines)",
                "Branch Coverage (project) - 20 of 100", "60.00% (40 missed branches)",
                "Mutation Coverage (project) - 20 of 100: 60.00% (40 survived mutations)",
                "Checkstyle (project) - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (project) - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)",
                "Cyclomatic Complexity (project): 10",
                "Cognitive Complexity (project): 100",
                "N-Path Complexity (project): <n/a>",
                "Non Commenting Source Statements (project): <n/a>");

        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score - 167 of 500 (33%)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score - 167 of 500 (33%)",
                "JUnit - 77 of 100",
                "JaCoCo - 40 of 100",
                "PIT - 20 of 100",
                "Style - 30 of 100",
                "Bugs - 0 of 100",
                "|Cyclomatic Complexity|project|10",
                "|Cognitive Complexity|project|100",
                "|Non Commenting Source Statements|project|-|-|-|-|-",
                "|N-Path Complexity|project|-|-|-|-|-");
    }

    void shouldCreateAllQualityResults() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThat(results.getMarkdownSummary(score, "Summary", true))
                .contains("JUnit Tests", "Code Coverage", "Mutation Coverage", "Style");
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .doesNotContain("JUnit Tests", "Code Coverage", "Style");

        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "Integrationstests (project): 55.56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests (project): 0.00% successful", "10 failed",
                "Line Coverage (project): 80.00% (20 missed lines)",
                "Branch Coverage (project): 60.00% (40 missed branches)",
                "Mutation Coverage (project): 60.00% (40 survived mutations)",
                "Checkstyle (project): 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (project): 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score",
                "|Integrationstests|project|12|5|3|4|:x:",
                "|Modultests|project|10|0|0|10|:x:",
                "|Checkstyle|project|10",
                "|SpotBugs|project|10",
                "|Line Coverage|project|80",
                "|Branch Coverage|project|60",
                "|Mutation Coverage|project|60");
    }

    void shouldCreateErrorReport() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createGradingAggregation();
        assertThat(results.getMarkdownErrors(score, new NoSuchElementException("This is an error")))
                .contains("# Partial score: 167/500",
                        "The grading has been aborted due to an error.",
                        "java.util.NoSuchElementException: This is an error");
    }

    void shouldCreateAnalysisResults() {
        var results = new GradingReport();

        var score = AnalysisMarkdownTest.createScoreForTwoResults();
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score - 60 of 200 (30%)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score - 60 of 200 (30%)",
                "|CheckStyle 1|project|10|30",
                "|CheckStyle 2|project|10|30",
                "Style - 60 of 100",
                "|SpotBugs 1|project|10|-120",
                "|SpotBugs 2|project|10|-120",
                "Bugs - 0 of 100");
    }

    void shouldSkipScores() {
        var configuration = NO_SCORE_CONFIGURATION;

        var results = new GradingReport();

        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore(logger);

        aggregation.gradeAnalysis(
                new ReportSupplier(AnalysisMarkdownTest::createTwoReports),
                AnalysisConfiguration.from(configuration));
        assertThat(logger.getInfoMessages()).contains(
                "Processing 2 static analysis configuration(s)",
                "=> Style: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "=> Bugs: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");

        aggregation.gradeTests(
                new NodeSupplier(TestMarkdownTest::createTwoReports),
                TestConfiguration.from(configuration));
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 test configuration(s)",
                "=> JUnit: 26.32% successful (14 failed, 5 passed, 3 skipped)");

        aggregation.gradeCoverage(
                new NodeSupplier(CoverageMarkdownTest::createTwoReports),
                CoverageConfiguration.from(configuration));
        assertThat(String.join("\n", logger.getInfoMessages())).contains(
                "Processing 2 coverage configuration(s)",
                "=> JaCoCo: 70.00% (60 missed items)",
                "=> PIT: 60.00% (40 survived mutations)"
        );

        assertThat(aggregation.getMetrics(Scope.PROJECT)).containsOnly(
                entry("tests", 19.0),
                entry("tests-success-rate", 5 / 19.0),
                entry("branch", 60.0),
                entry("line", 80.0),
                entry("mutation", 60.0),
                entry("style", 10.0),
                entry("bugs", 10.0),
                entry("checkstyle", 10.0),
                entry("spotbugs", 10.0));

        assertThat(results.getMarkdownSummary(aggregation, "Summary")).contains(
                "## :sunny: &nbsp; Summary",
                "Integrationstests (project): 55.56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests (project): 0.00% successful", "10 failed",
                "Branch Coverage (project): 60.00% (40 missed branches)",
                "Mutation Coverage (project): 60.00% (40 survived mutations)",
                "Checkstyle (project): 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs (project): 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(results.getTextSummary(aggregation)).isEqualTo(
                "Autograding score");
        assertThat(results.getTextSummary(aggregation, "Quality Summary")).isEqualTo(
                "Quality Summary");
        assertThat(results.getMarkdownDetails(aggregation, "Quality Summary")).contains(
                "# :sunny: &nbsp; Quality Summary",
                "JUnit",
                "JaCoCo",
                "PIT",
                "Style",
                "Bugs");
    }
}
