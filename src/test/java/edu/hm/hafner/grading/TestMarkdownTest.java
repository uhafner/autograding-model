package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Report;

import static edu.hm.hafner.grading.TestMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TestMarkdown}.
 *
 * @author Ullrich Hafner
 */
class TestMarkdownTest {
    @Test
    void shouldSkip() {
        TestMarkdown writer = new TestMarkdown();

        String markdown = writer.create(new AggregatedScore(), Collections.emptyList());

        assertThat(markdown).contains(TYPE + " not enabled");
    }

    @Test
    void shouldShowWrongConfiguration() {
        TestMarkdown writer = new TestMarkdown();

        String markdown = writer.create(createScore(), Collections.emptyList());

        assertThat(markdown).contains(TYPE + " enabled but no results found");
    }

    @Test
    void shouldShowMaximumScore() {
        TestMarkdown writer = new TestMarkdown();

        AggregatedScore score = createScore();
        score.addTestScores(new TestSupplier() {
            @Override
            protected List<TestScore> createScores(final TestConfiguration configuration) {
                TestScore empty = new TestScore.TestScoreBuilder()
                        .withDisplayName("Empty").withConfiguration(configuration).build();
                return Collections.singletonList(empty);
            }
        });
        String markdown = writer.create(score, Collections.singletonList(new Report()));

        assertThat(markdown).contains(TYPE + ": 100 of 100")
                .contains("|Empty|0|0|0|0")
                .doesNotContain("Total");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        TestMarkdown writer = new TestMarkdown();

        AggregatedScore score = createScore();
        score.addTestScores(new TestSupplier() {
            @Override
            protected List<TestScore> createScores(final TestConfiguration configuration) {
                return Collections.singletonList(createFirstScore(configuration));
            }
        });
        String markdown = writer.create(score, Collections.singletonList(new Report()));

        assertThat(markdown).contains(TYPE + ": 93 of 100")
                .contains("|First|3|2|1|-7")
                .contains("|*-*|*-*|*-1*|*-5*|*:moneybag:*")
                .doesNotContain("Total");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        TestMarkdown writer = new TestMarkdown();

        AggregatedScore score = createScore();
        score.addTestScores(new TestSupplier() {
            @Override
            protected List<TestScore> createScores(final TestConfiguration configuration) {
                return Arrays.asList(createFirstScore(configuration), createSecondScore(configuration));
            }
        });
        String markdown = writer.create(score, Collections.singletonList(new Report()));

        assertThat(markdown).contains(TYPE + ": 76 of 100")
                .contains("|First|3|2|1|-7")
                .contains("|Second|1|2|3|-17")
                .contains("|**Total**|**4**|**4**|**4**|**-24**");
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

    private TestScore createSecondScore(final TestConfiguration configuration) {
        return new TestScore.TestScoreBuilder()
                .withDisplayName("Second")
                .withConfiguration(configuration)
                .withFailedSize(3)
                .withSkippedSize(2)
                .withTotalSize(6)
                .build();
    }

    private AggregatedScore createScore() {
        return new AggregatedScore(
                "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-5,\"highImpact\":-3,\"normalImpact\":-2,\"lowImpact\":-1}, \"tests\":{\"maxScore\":100,\"passedImpact\":0,\"failureImpact\":-5,\"skippedImpact\":-1}, \"coverage\":{\"maxScore\":100,\"coveredPercentageImpact\":0,\"missedPercentageImpact\":-1}, \"pit\":{\"maxScore\":100,\"detectedImpact\":0,\"undetectedImpact\":0,\"undetectedPercentageImpact\":-1,\"detectedPercentageImpact\":0}}");
    }
}
