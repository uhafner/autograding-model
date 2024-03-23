package edu.hm.hafner.grading;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import static edu.hm.hafner.grading.assertions.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link GradingReport}.
 *
 * @author Ullrich Hafner
 */
class GradingReportTest {
    private static final String NO_SCORE_CONFIGURATION = """
            {
              "tests": [{
                "name": "JUnit",
                "tools": [
                  {
                    "name": "Integrationstests",
                    "id": "itest"
                  },
                  {
                    "name": "Modultests",
                    "id": "mtest"
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
                    "metric": "line"
                  },
                  {
                    "id": "jacoco",
                    "name": "Branch Coverage",
                    "metric": "branch"
                  }
                ]
              },
              {
                "name": "PIT",
                "tools" : [
                  {
                    "name": "Mutation Coverage",
                    "id": "pit",
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
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score",
                "Unit Tests Score: not enabled",
                "Coverage Score: not enabled",
                "Static Analysis Warnings Score: not enabled");
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .contains("Summary");
    }

    @Test
    void shouldCreateAllResults() {
        var results = new GradingReport();

        var score = new AggregatedScoreTest().createSerializable();
        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "img title=\"Score percentage: 33%\"",
                "percentages/033.svg",
                "# :mortar_board: &nbsp; Summary - 167 of 500",
                "JUnit - 77 of 100",
                "14 tests failed, 5 passed, 3 skipped",
                "JaCoCo - 40 of 100",
                "70% coverage achieved",
                "PIT - 20 of 100",
                "60% mutations killed",
                "Style - 30 of 100",
                "10 warnings found (1 error, 2 high, 3 normal, 4 low)",
                "Bugs - 0 of 100",
                "10 warnings found (4 errors, 3 high, 2 normal, 1 low)");
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
                ":warning: &nbsp; Style",
                "Bugs - 0 of 100",
                "title=\"Bugs: 0%\"",
                ":bug: &nbsp; Bugs");
    }

    @Test
    void shouldCreateErrorReport() {
        var results = new GradingReport();

        var score = new AggregatedScoreTest().createSerializable();
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
                "Unit Tests Score: not enabled",
                "Code Coverage Score: not enabled",
                "Mutation Coverage Score: not enabled",
                "|CheckStyle 1|1|1|2|3|4|10|30",
                "|CheckStyle 2|1|1|2|3|4|10|30",
                "Style - 60 of 100",
                "|SpotBugs 1|1|4|3|2|1|10|-120",
                "|SpotBugs 2|1|4|3|2|1|10|-120",
                "Bugs - 0 of 100");
    }

    @Test
    void shouldSkipScores() {
        var results = new GradingReport();

        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore(NO_SCORE_CONFIGURATION, logger);

        aggregation.gradeAnalysis((tool, log) -> AnalysisMarkdownTest.createTwoReports(tool));
        assertThat(logger.getInfoMessages()).contains(
                "Processing 2 static analysis configuration(s)",
                "=> Style: 10 warnings found (1 error, 2 high, 3 normal, 4 low)",
                "=> Bugs: 10 warnings found (4 errors, 3 high, 2 normal, 1 low)");

        aggregation.gradeTests((tool, log) -> TestMarkdownTest.createTwoReports(tool));
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 test configuration(s)",
                "=> JUnit: 14 tests failed, 5 passed, 3 skipped");

        aggregation.gradeCoverage((tool, log) -> CoverageMarkdownTest.createTwoReports(tool));
        assertThat(String.join("\n", logger.getInfoMessages())).contains(
                "Processing 2 coverage configuration(s)",
                "=> JaCoCo: 70% coverage achieved",
                "=> PIT: 60% mutations killed"
        );

        assertThat(aggregation.getMetrics()).containsOnly(
                entry("tests", 22),
                entry("branch", 60),
                entry("line", 80),
                entry("mutation", 60),
                entry("style", 10),
                entry("bugs", 10),
                entry("checkstyle", 10),
                entry("spotbugs", 10));

        assertThat(results.getMarkdownSummary(aggregation, "Summary")).contains(
                "### :sunny: &nbsp; Summary",
                "- :vertical_traffic_light: &nbsp; JUnit: 14 tests failed, 5 passed, 3 skipped",
                "- :footprints: &nbsp; JaCoCo: 70% coverage achieved",
                "- :microscope: &nbsp; PIT: 60% mutations killed",
                "- :warning: &nbsp; Style: 10 warnings found (1 error, 2 high, 3 normal, 4 low)",
                "- :bug: &nbsp; Bugs: 10 warnings found (4 errors, 3 high, 2 normal, 1 low)");
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
