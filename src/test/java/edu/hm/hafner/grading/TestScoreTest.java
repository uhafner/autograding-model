package edu.hm.hafner.grading;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.TestScore.TestScoreBuilder;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class TestScoreTest {
    private static final String NAME = "Tests";
    private static final String ID = "tests";

    @Test
    void shouldCalculateImpactAndScoreWithNegativeValues() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "passedImpact": -10,
                    "failureImpact": -5,
                    "skippedImpact": -2,
                    "maxScore": 100
                  }
                }
                """);

        var testScore = new TestScoreBuilder()
                .withId(ID)
                .withName(NAME)
                .withConfiguration(configuration)
                .withFailedSize(5)
                .withSkippedSize(3)
                .withTotalSize(10)
                .build();
        assertThat(testScore)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(100)
                .hasImpact(-5 * 5 - 3 * 2 - 2 * 10)
                .hasValue(49);

        assertThat(testScore.toString()).startsWith("{").endsWith("}").contains("\"impact\":-51");
    }

    @Test
    void shouldCalculateImpactAndScoreWithPositiveValues() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "passedImpact": 10,
                    "failureImpact": 5,
                    "skippedImpact": 2,
                    "maxScore": 100
                  }
                }
                """);

        var testScore = new TestScoreBuilder()
                .withId(ID)
                .withName(NAME)
                .withConfiguration(configuration)
                .withFailedSize(5)
                .withSkippedSize(3)
                .withTotalSize(10)
                .build();
        assertThat(testScore)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(100)
                .hasImpact(5 * 5 + 3 * 2 + 2 * 10)
                .hasValue(51);

        assertThat(testScore.toString()).startsWith("{").endsWith("}").contains("\"impact\":51");
    }

    @Test
    void shouldComputePositiveImpactBySizeZero() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "name": "JUnit Test Results",
                    "passedImpact": 100,
                    "failureImpact": 100,
                    "skippedImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withFailedSize(0)
                .withSkippedSize(0)
                .withTotalSize(0)
                .build();
        assertThat(score)
                .hasImpact(0)
                .hasValue(0)
                .hasId("tests")
                .hasName("JUnit Test Results");
    }

    @Test
    void shouldComputeNegativeImpactBySizeZero() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "name": "JUnit Test Results",
                    "passedImpact": -100,
                    "failureImpact": -100,
                    "skippedImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withFailedSize(0)
                .withSkippedSize(0)
                .withTotalSize(0)
                .build();
        assertThat(score)
                .hasImpact(0)
                .hasValue(50)
                .hasId("tests")
                .hasName("JUnit Test Results");
    }

    @Test
    void shouldHandleOverflowWithPositiveImpact() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "passedImpact": 100,
                    "failureImpact": 100,
                    "skippedImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withFailedSize(10)
                .withSkippedSize(20)
                .withTotalSize(30)
                .build();
        assertThat(score)
                .hasImpact(3000)
                .hasValue(50);
    }

    @Test
    void shouldHandleOverflowWithNegativeImpact() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "passedImpact": -100,
                    "failureImpact": -100,
                    "skippedImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withFailedSize(10)
                .withSkippedSize(20)
                .withTotalSize(30)
                .build();
        assertThat(score)
                .hasImpact(-3000)
                .hasValue(0);
    }

    @Test
    void shouldCreateSubScores() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "passedImpact": 3,
                    "failureImpact": -10,
                    "skippedImpact": -1,
                    "maxScore": 200
                  }
                }
                """);

        var builder = new TestScoreBuilder()
                .withConfiguration(configuration);
        var first = builder.withFailedSize(3)
                .withSkippedSize(4)
                .withTotalSize(10)
                .build();
        assertThat(first).hasImpact(-25).hasValue(175);
        var second = builder.withFailedSize(7)
                .withSkippedSize(6)
                .withTotalSize(20)
                .build();
        assertThat(second).hasImpact(-55).hasValue(145);

        var aggregation = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withScores(List.of(first, second))
                .withName("Aggregation")
                .withId("aggregation")
                .build();
        assertThat(aggregation)
                .hasImpact(-25 - 55)
                .hasValue(75 + 45)
                .hasId("aggregation")
                .hasName("Aggregation")
                .hasOnlySubScores(first, second);

        var overflow = new TestScoreBuilder()
                .withConfiguration(createConfiguration("""
                {
                  "tests": {
                    "passedImpact": -1,
                    "failureImpact": -1,
                    "skippedImpact": -1,
                    "maxScore": 20
                  }
                }
                """))
                .withScores(List.of(first, second))
                .withName("Aggregation")
                .withId("aggregation")
                .build();
        assertThat(overflow).hasImpact(-30).hasValue(0).hasId("aggregation").hasName("Aggregation");
    }

    private TestConfiguration createConfiguration(final String json) {
        return TestConfiguration.from(json).get(0);
    }
}
