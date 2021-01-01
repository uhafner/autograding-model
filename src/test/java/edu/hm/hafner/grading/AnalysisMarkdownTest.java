package edu.hm.hafner.grading;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.grading.AnalysisMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link AnalysisMarkdown}.
 *
 * @author Ullrich Hafner
 */
class AnalysisMarkdownTest {
    @Test
    void shouldSkip() {
        AnalysisMarkdown writer = new AnalysisMarkdown();

        String markdown = writer.create(new AggregatedScore());

        assertThat(markdown).contains(TYPE + " not enabled");
    }

    @Test
    void shouldShowWrongConfiguration() {
        AnalysisMarkdown writer = new AnalysisMarkdown();

        String markdown = writer.create(createScore());

        assertThat(markdown).contains(TYPE + " enabled but no results found");
    }

    @Test
    void shouldShowMaximumScore() {
        AnalysisMarkdown writer = new AnalysisMarkdown();

        AggregatedScore score = createScore();
        score.addAnalysisScores(new AnalysisSupplier() {
            @Override
            protected List<AnalysisScore> createScores(final AnalysisConfiguration configuration) {
                AnalysisScore empty = new AnalysisScore.AnalysisScoreBuilder().withId("Empty")
                        .withDisplayName("Empty").withConfiguration(configuration).build();
                return Collections.singletonList(empty);
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 100 of 100")
                .contains("|Empty|0|0|0|0|0");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        AnalysisMarkdown writer = new AnalysisMarkdown();

        AggregatedScore score = createScore();
        score.addAnalysisScores(new AnalysisSupplier() {
            @Override
            protected List<AnalysisScore> createScores(final AnalysisConfiguration configuration) {
                return Collections.singletonList(createFirstScore(configuration));
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 79 of 100")
                .contains("|First|1|2|3|4|-21");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        AnalysisMarkdown writer = new AnalysisMarkdown();

        AggregatedScore score = createScore();
        score.addAnalysisScores(new AnalysisSupplier() {
            @Override
            protected List<AnalysisScore> createScores(final AnalysisConfiguration configuration) {
                return Arrays.asList(createFirstScore(configuration), createSecondScore(configuration));
            }
        });
        String markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 45 of 100")
                .contains("|First|1|2|3|4|-21")
                .contains("|Second|4|3|2|1|-34");
    }

    private AnalysisScore createFirstScore(final AnalysisConfiguration configuration) {
        return new AnalysisScore.AnalysisScoreBuilder().withId("First")
                .withDisplayName("First")
                .withConfiguration(configuration)
                .withTotalErrorsSize(1)
                .withTotalHighSeveritySize(2)
                .withTotalNormalSeveritySize(3)
                .withTotalLowSeveritySize(4)
                .build();
    }

    private AnalysisScore createSecondScore(final AnalysisConfiguration configuration) {
        return new AnalysisScore.AnalysisScoreBuilder().withId("Second")
                .withDisplayName("Second")
                .withConfiguration(configuration)
                .withTotalErrorsSize(4)
                .withTotalHighSeveritySize(3)
                .withTotalNormalSeveritySize(2)
                .withTotalLowSeveritySize(1)
                .build();
    }

    private AggregatedScore createScore() {
        return new AggregatedScore(
                "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-5,\"highImpact\":-3,\"normalImpact\":-2,\"lowImpact\":-1}, \"tests\":{\"maxScore\":100,\"passedImpact\":0,\"failureImpact\":-5,\"skippedImpact\":-1}, \"coverage\":{\"maxScore\":100,\"coveredPercentageImpact\":0,\"missedPercentageImpact\":-1}, \"pit\":{\"maxScore\":100,\"detectedImpact\":0,\"undetectedImpact\":0,\"undetectedPercentageImpact\":-1,\"detectedPercentageImpact\":0}}");
    }
}
