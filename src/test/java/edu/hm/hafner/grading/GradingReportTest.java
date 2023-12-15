package edu.hm.hafner.grading;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link GradingReport}.
 *
 * @author Ullrich Hafner
 */
class GradingReportTest {
    @Test
    void shouldCreateEmptyResults() {
        var results = new GradingReport();

        var score = new AggregatedScore();
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Total score - 0 of 0");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Total score - 0 of 0",
                "Unit Tests Score: not enabled",
                "Coverage Score: not enabled",
                "Static Analysis Warnings Score: not enabled");
        assertThat(results.getMarkdownSummary(score, "Summary"))
                .contains("Summary - 0 of 0");
    }

    @Test
    void shouldCreateAllResults() {
        var results = new GradingReport();

        var score = new AggregatedScoreTest().createSerializable();
        assertThat(results.getMarkdownSummary(score, ":mortar_board: Summary")).contains(
                "# :mortar_board: Summary - 167 of 500",
                "JUnit - 77 of 100",
                "14 tests failed, 5 passed, 3 skipped",
                "JaCoCo - 40 of 100",
                "70% Covered , 30% Missed",
                "PIT - 20 of 100",
                "60% Killed , 40% Survived",
                "Style - 30 of 100",
                "10 warnings found (1 errors, 2 high, 3 normal, 4 low)",
                "Bugs - 0 of 100",
                "10 warnings found (4 errors, 3 high, 2 normal, 1 low)");
        assertThat(results.getTextSummary(score)).isEqualTo(
                "Total score - 167 of 500 (unit tests: 77/100, code coverage: 40/100, mutation coverage: 20/100, analysis: 30/200)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Total score - 167 of 500",
                "JUnit - 77 of 100",
                "JaCoCo - 40 of 100",
                "PIT - 20 of 100",
                "Style - 30 of 100",
                ":warning: Style",
                "Bugs - 0 of 100",
                ":bug: Bugs");
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
                "Total score - 30 of 200 (analysis: 30/200)");
        assertThat(results.getMarkdownDetails(score)).contains(
                "Total score - 30 of 200",
                "Unit Tests Score: not enabled",
                "Code Coverage Score: not enabled",
                "Mutation Coverage Score: not enabled",
                "|CheckStyle|1|2|3|4|10|30",
                "Style - 30 of 100",
                "|SpotBugs|4|3|2|1|10|-120",
                "Bugs - 0 of 100");
    }
}
