package edu.hm.hafner.grading;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link GradingReport}.
 *
 * @author Ullrich Hafner
 */
// TODO: The report header should evaluate which elements are actually enabled
class GradingReportTest {
    @Test
    void shouldCreateEmptyResults() {
        GradingReport results = new GradingReport();

        AggregatedScore score = new AggregatedScore();
        assertThat(results.getSummary(score))
                .contains("Total score: 0/0")
                .contains("unit tests: 0/0")
                .contains("code coverage: 0/0")
                .contains("mutation coverage: 0/0")
                .contains("analysis: 0/0");
        assertThat(results.getDetails(score, Collections.emptyList()))
                .contains("# Total score: 0/0")
                .contains("Unit Tests Score not enabled")
                .contains("Code Coverage Score not enabled")
                .contains("PIT Mutation Coverage Score not enabled")
                .contains("Static Analysis Warnings Score");
    }
}
