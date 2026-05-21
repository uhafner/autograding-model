package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import edu.hm.hafner.util.FilteredLog;

import java.util.NoSuchElementException;

import static edu.hm.hafner.grading.ScoreBuilder.*;
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
        var disabledScores = new String[] {
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
                "# :mortar_board: &nbsp; Summary - 116 of 500 (23%)",
                "Integrationstests (Whole Project) - 56 of 100: 55.56% successful",
                "Modultests (Whole Project) - 0 of 100: 0.00% successful — 10 failed",
                "Line Coverage (Whole Project) - 60 of 100: 80.00% — 20 missed lines",
                "Branch Coverage (Whole Project) - 20 of 100: 60.00% — 40 missed branches",
                "Mutation Coverage (Whole Project) - 20 of 100: 60.00% — 40 survived mutations",
                "Checkstyle (Whole Project) - 30 of 100: 10 warnings — error: 1, high: 2, normal: 3, low: 4",
                "SpotBugs (Whole Project) - 0 of 100: 10 bugs — error: 4, high: 3, normal: 2, low: 1",
                "Cyclomatic Complexity (Whole Project): 10",
                "Cognitive Complexity (Whole Project): 100",
                "N-Path Complexity (Whole Project): <n/a>",
                "Non Commenting Source Statements (Whole Project): <n/a>");

        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score - 116 of 500 (23%)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score - 116 of 500 (23%)",
                "JUnit - 26 of 100",
                "JaCoCo - 40 of 100",
                "PIT - 20 of 100",
                "Style - 30 of 100",
                "Bugs - 0 of 100",
                "|Cyclomatic Complexity|Whole Project|10",
                "|Cognitive Complexity|Whole Project|100",
                "|Non Commenting Source Statements|Whole Project|-|-|-|-|-",
                "|N-Path Complexity|Whole Project|-|-|-|-|-");
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
                "Integrationstests (Whole Project): ❌&nbsp;unstable — 4 failed, 5 passed, 3 skipped",
                "Modultests (Whole Project): ❌&nbsp;unstable — 10 failed",
                "Line Coverage (Whole Project): 80.00% — 20 missed lines",
                "Branch Coverage (Whole Project): 60.00% — 40 missed branches",
                "Mutation Coverage (Whole Project): 60.00% — 40 survived mutations",
                "Checkstyle (Whole Project): 10 warnings — error: 1, high: 2, normal: 3, low: 4",
                "SpotBugs (Whole Project): 10 bugs — error: 4, high: 3, normal: 2, low: 1");
        assertThatReferenceIsMissing(results, score);
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Autograding score");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Autograding score",
                "|Integrationstests|Whole Project|5|3|4|:x:",
                "|Modultests|Whole Project|0|0|10|:x:\n"
                        + "|**Total**|**-**|**-**|**5**|**3**|**14**|:x:",
                "|Checkstyle|Whole Project|10",
                "|SpotBugs|Whole Project|10",
                "|Line Coverage|Whole Project|80",
                "|Branch Coverage|Whole Project|60",
                "|Mutation Coverage|Whole Project|60");
    }

    @Test
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "https://github.com/uhafner/autograding-model/commit/1234567890")
    @SetEnvironmentVariable(key = "RUN_URL", value = "https://github.com/uhafner/autograding-model/actions/runs/1")
    void shouldCreateAllQualityResultsWithLinks() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "Delta reports computed against the reference results of ",
                "https://github.com/uhafner/autograding-model/commit/1234567890",
                "in [workflow run 1](https://github.com/uhafner/autograding-model/actions/runs/1)");
        assertThat(results.getMarkdownDetails(score, "Summary")).contains(
                "Delta reports computed against the reference results of ",
                "https://github.com/uhafner/autograding-model/commit/1234567890",
                "in [workflow run 1](https://github.com/uhafner/autograding-model/actions/runs/1)");
    }

    @Test
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "invalid-url")
    @SetEnvironmentVariable(key = "RUN_URL", value = "https://github.com/uhafner/autograding-model/actions/runs/1")
    void shouldNotCreateLinksForInvalidCommitUrl() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThatReferenceIsMissing(results, score);
    }

    @Test
    @SetEnvironmentVariable(key = "RUN_URL", value = "invalid-url")
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "https://github.com/uhafner/autograding-model/actions/runs/1")
    void shouldNotCreateLinksForInvalidRunUrls() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThatReferenceIsMissing(results, score);
    }

    @Test
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "")
    @SetEnvironmentVariable(key = "RUN_URL", value = "https://github.com/uhafner/autograding-model/actions/runs/1")
    void shouldNotCreateLinksForEmptyCommitUrl() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThatReferenceIsMissing(results, score);
    }

    @Test
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "https://github.com/uhafner/autograding-model/commit/1234567890")
    void shouldNotCreateLinksForMissingRunUrl() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThatReferenceIsMissing(results, score);
    }

    @Test
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "https://github.com/uhafner/autograding-model/commit/1234567890")
    @SetEnvironmentVariable(key = "RUN_URL", value = "")
    void shouldNotCreateLinksForEmptyRunUrl() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThatReferenceIsMissing(results, score);
    }

    private void assertThatReferenceIsMissing(final GradingReport results, final AggregatedScore score) {
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .doesNotContain("## :pushpin: Reference Results");
        assertThat(results.getMarkdownDetails(score, "Summary"))
                .doesNotContain("## :pushpin: Reference Results");
    }

    @Test
    void shouldCreateErrorReport() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createGradingAggregation();
        assertThat(results.getMarkdownErrors(score, new NoSuchElementException("This is an error")))
                .contains("# Partial score: 116/500",
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
                "|CheckStyle 1|Whole Project|10|30",
                "|CheckStyle 2|Whole Project|10|30",
                "Style - 60 of 100",
                "|SpotBugs 1|Whole Project|10|-120",
                "|SpotBugs 2|Whole Project|10|-120",
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
                AnalysisConfiguration.from(configuration), NO_DELTA_REPORTS);
        assertThat(logger.getInfoMessages()).contains(
                "Processing 2 static analysis configuration(s)",
                "=> Style: 10 warnings (error: 1, high: 2, normal: 3, low: 4) [Whole Project]",
                "=> Bugs: 10 bugs (error: 4, high: 3, normal: 2, low: 1) [Whole Project]");

        aggregation.gradeTests(
                new NodeSupplier(TestMarkdownTest::createTwoReports),
                TestConfiguration.from(configuration), NO_DELTA_REPORTS);
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 test configuration(s)",
                "=> JUnit: 26.32% successful (14 failed, 5 passed, 3 skipped) [Whole Project]");

        aggregation.gradeCoverage(
                new NodeSupplier(CoverageMarkdownTest::createTwoReports),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);
        assertThat(String.join("\n", logger.getInfoMessages())).contains(
                "Processing 2 coverage configuration(s)",
                "=> JaCoCo: 70.00% (60 missed items) [Whole Project]",
                "=> PIT: 60.00% (40 survived mutations) [Whole Project]"
        );

        assertThat(aggregation.getMetrics(Scope.PROJECT)).containsOnly(
                entry("tests", 19.0),
                entry("test-success-rate", 26.32),
                entry("branch", 60.0),
                entry("line", 80.0),
                entry("mutation", 60.0),
                entry("style", 10.0),
                entry("bugs", 10.0),
                entry("checkstyle", 10.0),
                entry("spotbugs", 10.0));

        assertThat(results.getMarkdownSummary(aggregation, "Summary")).contains(
                "## :sunny: &nbsp; Summary",
                "Integrationstests (Whole Project): ❌&nbsp;unstable", "4 failed", "5 passed", "3 skipped",
                "Modultests (Whole Project): ❌&nbsp;unstable", "10 failed",
                "Line Coverage (Whole Project): 80.00% — 20 missed lines",
                "Branch Coverage (Whole Project): 60.00% — 40 missed branches",
                "Mutation Coverage (Whole Project): 60.00% — 40 survived mutations",
                "Checkstyle (Whole Project): 10 warnings — error: 1, high: 2, normal: 3, low: 4",
                "SpotBugs (Whole Project): 10 bugs — error: 4, high: 3, normal: 2, low: 1");
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
