package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.grading.PitMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link PitMarkdown}.
 *
 * @author Ullrich Hafner
 */
class PitMarkdownTest {
    @Test
    void shouldSkip() {
        PitMarkdown writer = new PitMarkdown();

        String markdown = writer.create(new AggregatedScore());

        assertThat(markdown).contains(TYPE + " not enabled");
    }

    @Test
    void shouldShowWrongConfiguration() {
        PitMarkdown writer = new PitMarkdown();

        String markdown = writer.create(createScore());

        assertThat(markdown).contains(TYPE + " enabled but no results found");
    }

    @Test
    void shouldShowMaximumScore() {
        PitMarkdown writer = new PitMarkdown();

        AggregatedScore score = createScore();
        score.addPitScores(new PitSupplier() {
            @Override
            protected List<PitScore> createScores(final PitConfiguration configuration) {
                PitScore empty = new PitScore.PitScoreBuilder()
                        .withDisplayName("Empty").withConfiguration(configuration).withTotalMutations(10).build();
                return Collections.singletonList(empty);
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 100 of 100")
                .contains("|10|0|100|0|0");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        PitMarkdown writer = new PitMarkdown();

        AggregatedScore score = createScore();
        score.addPitScores(new PitSupplier() {
            @Override
            protected List<PitScore> createScores(final PitConfiguration configuration) {
                return Collections.singletonList(createFirstScore(configuration));
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 67 of 100")
                .contains("|10|5|67|33|-33")
                .contains("|*0*|*0*|*0*|*-1*|*:moneybag:*");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        PitMarkdown writer = new PitMarkdown();

        AggregatedScore score = createScore();
        score.addPitScores(new PitSupplier() {
            @Override
            protected List<PitScore> createScores(final PitConfiguration configuration) {
                return Arrays.asList(createFirstScore(configuration), createSecondScore(configuration));
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 0 of 100")
                .contains("|10|5|67|33|-33")
                .contains("|20|80|20|80|-80");
    }

    private PitScore createFirstScore(final PitConfiguration configuration) {
        return new PitScore.PitScoreBuilder()
                .withDisplayName("First")
                .withConfiguration(configuration)
                .withUndetectedMutations(5)
                .withTotalMutations(15)
                .build();
    }

    private PitScore createSecondScore(final PitConfiguration configuration) {
        return new PitScore.PitScoreBuilder()
                .withDisplayName("Second")
                .withConfiguration(configuration)
                .withUndetectedMutations(80)
                .withTotalMutations(100)
                .build();
    }

    private AggregatedScore createScore() {
        return new AggregatedScore(
                "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-5,\"highImpact\":-3,\"normalImpact\":-2,\"lowImpact\":-1}, \"tests\":{\"maxScore\":100,\"passedImpact\":0,\"failureImpact\":-5,\"skippedImpact\":-1}, \"coverage\":{\"maxScore\":100,\"coveredPercentageImpact\":0,\"missedPercentageImpact\":-1}, \"pit\":{\"maxScore\":100,\"detectedImpact\":0,\"undetectedImpact\":0,\"undetectedPercentageImpact\":-1,\"detectedPercentageImpact\":0}}");
    }
}
