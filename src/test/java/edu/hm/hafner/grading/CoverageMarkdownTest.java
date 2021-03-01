package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.grading.CoverageMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageMarkdown}.
 *
 * @author Ullrich Hafner
 */
class CoverageMarkdownTest {
    @Test
    void shouldSkip() {
        CoverageMarkdown writer = new CoverageMarkdown();

        String markdown = writer.create(new AggregatedScore());

        assertThat(markdown).contains(TYPE + " not enabled");
    }

    @Test
    void shouldShowWrongConfiguration() {
        CoverageMarkdown writer = new CoverageMarkdown();

        String markdown = writer.create(createScore());

        assertThat(markdown).contains(TYPE + " enabled but no results found");
    }

    @Test
    void shouldShowMaximumScore() {
        CoverageMarkdown writer = new CoverageMarkdown();

        AggregatedScore score = createScore();
        score.addCoverageScores(new CoverageSupplier() {
            @Override
            protected List<CoverageScore> createScores(final CoverageConfiguration configuration) {
                CoverageScore empty = new CoverageScore.CoverageScoreBuilder().withId("Empty")
                        .withDisplayName("Empty").withConfiguration(configuration).withCoveredPercentage(100).build();
                return Collections.singletonList(empty);
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 100 of 100")
                .contains("|Empty|100|0");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        CoverageMarkdown writer = new CoverageMarkdown();

        AggregatedScore score = createScore();
        score.addCoverageScores(new CoverageSupplier() {
            @Override
            protected List<CoverageScore> createScores(final CoverageConfiguration configuration) {
                return Collections.singletonList(createFirstScore(configuration));
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 10 of 100")
                .contains("|First|10|90|-90")
                .contains("|*:moneybag:*|*-*|*-1*|*:ledger:*");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        CoverageMarkdown writer = new CoverageMarkdown();

        AggregatedScore score = createScore();
        score.addCoverageScores(new CoverageSupplier() {
            @Override
            protected List<CoverageScore> createScores(final CoverageConfiguration configuration) {
                return Arrays.asList(createFirstScore(configuration), createSecondScore(configuration));
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 0 of 100")
                .contains("|First|10|90|-90")
                .contains("|Second|80|20|-20")
                .contains("|**Total**|**45**|**55**|**-110**");
    }


    private CoverageScore createFirstScore(final CoverageConfiguration configuration) {
        return new CoverageScore.CoverageScoreBuilder().withId("First")
                .withDisplayName("First")
                .withConfiguration(configuration)
                .withCoveredPercentage(10)
                .build();
    }

    private CoverageScore createSecondScore(final CoverageConfiguration configuration) {
        return new CoverageScore.CoverageScoreBuilder().withId("Second")
                .withDisplayName("Second")
                .withConfiguration(configuration)
                .withCoveredPercentage(80)
                .build();
    }

    private AggregatedScore createScore() {
        return new AggregatedScore(
                "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-5,\"highImpact\":-3,\"normalImpact\":-2,\"lowImpact\":-1}, \"tests\":{\"maxScore\":100,\"passedImpact\":0,\"failureImpact\":-5,\"skippedImpact\":-1}, \"coverage\":{\"maxScore\":100,\"coveredPercentageImpact\":0,\"missedPercentageImpact\":-1}, \"pit\":{\"maxScore\":100,\"detectedImpact\":0,\"undetectedImpact\":0,\"undetectedPercentageImpact\":-1,\"detectedPercentageImpact\":0}}");
    }
}
