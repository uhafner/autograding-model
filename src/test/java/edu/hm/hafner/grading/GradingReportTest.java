package edu.hm.hafner.grading;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.Report;

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

    @Test
    void shouldTruncateResults() {
        GradingReport results = new GradingReport();

        AggregatedScore score = new AggregatedScore("{\"tests\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"passedImpact\": 0,\n"
                + "    \"failureImpact\": -5,\n"
                + "    \"skippedImpact\": -1\n"
                + "  }}");
        Report report = new Report();
        for (int i = 0; i < 60000; i++) {
            report.add(new IssueBuilder().setFileName(String.valueOf(i)).build());
        }

        score.addTestScores(new TestSupplier() {
            @Override
            protected List<TestScore> createScores(final TestConfiguration configuration) {
                return Collections.singletonList(createFirstScore(configuration));
            }
        });
        assertThat(results.getDetails(score, Collections.singletonList(report)))
                .contains(TestMarkdown.TRUNCATED_MESSAGE)
                .hasSizeLessThan(65535);
    }

    private TestScore createFirstScore(final TestConfiguration configuration) {
        return new TestScore.TestScoreBuilder()
                .withDisplayName("First")
                .withConfiguration(configuration)
                .withFailedSize(1)
                .withSkippedSize(2)
                .withTotalSize(6)
                .build();
    }
}
