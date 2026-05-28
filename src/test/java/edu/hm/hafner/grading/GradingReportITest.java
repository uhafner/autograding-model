package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link GradingReport}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class GradingReportITest {
    @Test
    @SetEnvironmentVariable(key = "COMMIT_URL", value = "https://github.com/uhafner/autograding-model/commit/1234567890")
    @SetEnvironmentVariable(key = "RUN_URL", value = "https://github.com/uhafner/autograding-model/actions/runs/1")
    void shouldCreateAllQualityResultsWithLinks() {
        var results = new GradingReport();

        var score = AggregatedScoreTest.createQualityAggregation();

        assertThat(results.getMarkdownSummary(score, "Summary")).contains(
                "Delta reports computed against the reference results of ",
                "https://github.com/uhafner/autograding-model/commit/1234567890",
                "in workflow run [1](https://github.com/uhafner/autograding-model/actions/runs/1)");
        assertThat(results.getMarkdownDetails(score, "Summary")).contains(
                "Delta reports computed against the reference results of ",
                "https://github.com/uhafner/autograding-model/commit/1234567890",
                "in workflow run [1](https://github.com/uhafner/autograding-model/actions/runs/1)");
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
}
