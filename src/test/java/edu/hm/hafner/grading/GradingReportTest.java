package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.util.FilteredLog;

import java.util.NoSuchElementException;

import static edu.hm.hafner.grading.assertions.Assertions.*;

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

    @Test
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
        assertThat(results.getMarkdownDetails(score, "Title", true))
                .contains("Title")
                .contains(disabledScores);
        assertThat(results.getMarkdownDetails(score, "Title"))
                .contains("Title")
                .doesNotContain(disabledScores);
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .contains("Summary");
    }

    @Test
    void shouldCreateAllGradingResults() {
        var results = new GradingReport();

        var score = new AggregatedScoreTest().createSerializable();
        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "img title=\"Score percentage: 33%\"",
                "percentages/033.svg",
                "# :mortar_board: &nbsp; Summary - 167 of 500",
                "Integrationstests - 27 of 100: 56% successful",
                "Modultests - 50 of 100:  0% successful",
                "Line Coverage - 60 of 100", "80% (20 missed lines)",
                "Branch Coverage - 20 of 100", "60% (40 missed branches)",
                "Mutation Coverage - 20 of 100: 60% (40 survived mutations)",
                "Checkstyle - 30 of 100: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs - 0 of 100: 10 bugs (error: 4, high: 3, normal: 2, low: 1)",
                "Cyclomatic Complexity: 10",
                "Cognitive Complexity: 100",
                "N-Path Complexity: <n/a>",
                "Non Commenting Source Statements: <n/a>");

        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score - 167 of 500 (33%)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score - 167 of 500 (33%)",
                "JUnit - 77 of 100",
                "title=\"JUnit: 77%\"",
                "JaCoCo - 40 of 100",
                "title=\"JaCoCo: 40%\"",
                "PIT - 20 of 100",
                "title=\"PIT: 20%\"",
                "Style - 30 of 100",
                "title=\"Style: 30%\"",
                "Bugs - 0 of 100",
                "title=\"Bugs: 0%\"",
                "|Cyclomatic Complexity|10",
                "|Cognitive Complexity|100",
                "|Non Commenting Source Statements|-|-|-|-|-",
                "|N-Path Complexity|-|-|-|-|-");
    }

    @Test
    void shouldCreateAllQualityResults() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThat(results.getMarkdownSummary(score, "Summary", true))
                .contains("JUnit Tests", "Code Coverage", "Mutation Coverage", "Style");
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .doesNotContain("JUnit Tests", "Code Coverage", "Style");

        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "Integrationstests: 56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests:  0% successful", "10 failed",
                "Line Coverage: 80% (20 missed lines)",
                "Branch Coverage: 60% (40 missed branches)",
                "Mutation Coverage: 60% (40 survived mutations)",
                "Checkstyle: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score",
                "|Integrationstests|1|5|3|4|12",
                "|Modultests|1|0|0|10|10",
                "|Checkstyle|1|1|2|3|4|10",
                "|SpotBugs|1|4|3|2|1|10",
                "|Line Coverage|80|20",
                "|Branch Coverage|60|40",
                "|Mutation Coverage|60|40");
    }

    @Test
    void shouldCreateErrorReport() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createGradingAggregation();
        assertThat(results.getMarkdownErrors(score, new NoSuchElementException("This is an error")))
                .contains("# Partial score: 167/500",
                        "The grading has been aborted due to an error.",
                        "java.util.NoSuchElementException: This is an error");
    }

    @Test
    void shouldCreateAnalysisResults() {
        var results = new GradingReport();

        var score = AnalysisMarkdownTest.createScoreForTwoResults();
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score - 60 of 200 (30%)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score - 60 of 200 (30%)",
                "|CheckStyle 1|1|1|2|3|4|10|30",
                "|CheckStyle 2|1|1|2|3|4|10|30",
                "Style - 60 of 100",
                "|SpotBugs 1|1|4|3|2|1|10|-120",
                "|SpotBugs 2|1|4|3|2|1|10|-120",
                "Bugs - 0 of 100");
    }

    @Test
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
                "=> JUnit: 14 tests failed, 5 passed, 3 skipped");

        aggregation.gradeCoverage(
                new NodeSupplier(CoverageMarkdownTest::createTwoReports),
                CoverageConfiguration.from(configuration));
        assertThat(String.join("\n", logger.getInfoMessages())).contains(
                "Processing 2 coverage configuration(s)",
                "=> JaCoCo: 70% (60 missed items)",
                "=> PIT: 60% (40 survived mutations)"
        );

        assertThat(aggregation.getMetrics()).containsOnly(
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
                "Integrationstests: 56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests:  0% successful", "10 failed",
                "Branch Coverage: 60% (40 missed branches)",
                "Mutation Coverage: 60% (40 survived mutations)",
                "Checkstyle: 10 warnings (error: 1, high: 2, normal: 3, low: 4)",
                "SpotBugs: 10 bugs (error: 4, high: 3, normal: 2, low: 1)");
        assertThat(results.getTextSummary(aggregation)).isEqualTo(
                "Autograding score");
        assertThat(results.getTextSummary(aggregation, "Quality Summary")).isEqualTo(
                "Quality Summary");
        assertThat(results.getMarkdownDetails(aggregation, "Quality Summary")).contains(
                "# :sunny: &nbsp; Quality Summary",
                "JUnit",
                "JaCoCo",
                "title=\"JaCoCo: 70%\"",
                "PIT",
                "title=\"PIT: 60%\"",
                "Style",
                "Bugs");
    }
}
