package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Rate;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.grading.TestScore.TestScoreBuilder;

import java.util.List;
import java.util.Locale;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class TestScoreTest {
    private static final String NAME = "Tests";

    @Test
    void shouldUseRelativeValues() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "maxScore": 100,
                    "successRateImpact": 1,
                    "failureRateImpact": 0
                  }
                }
                """);

        var builder = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration);
        var twenty = builder.create(createTestReport(2, 3, 5), Metric.TESTS);
        assertThat(twenty)
                .hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(100)
                .hasImpact(29)
                .hasValue(29);
        assertThat(twenty.toString()).startsWith("{").endsWith("}")
                .containsIgnoringWhitespaces("\"impact\":29");

        assertThat(builder.create(createTestReport(12, 3, 5), Metric.TESTS))
                .hasImpact(71).hasValue(71);
        assertThat(builder.create(createTestReport(95, 5, 0), Metric.TESTS))
                .hasImpact(100).hasValue(100);
        assertThat(builder.create(createTestReport(100, 0, 0), Metric.TESTS))
                .hasImpact(100).hasValue(100);

        // Check rounding
        assertThat(builder.create(createTestReport(197, 0, 3), Metric.TESTS))
                .hasImpact(99).hasValue(99);
        assertThat(builder.create(createTestReport(198, 0, 2), Metric.TESTS))
                .hasImpact(99).hasValue(99);
        assertThat(builder.create(createTestReport(199, 0, 1), Metric.TESTS))
                .hasImpact(100).hasValue(100);
        assertThat(builder.create(createTestReport(200, 0, 0), Metric.TESTS))
                .hasImpact(100).hasValue(100);
    }

    @Test
    void shouldScaleRelativeValuesWithMaxScore() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "maxScore": 50,
                    "successRateImpact": 1,
                    "failureRateImpact": 0
                  }
                }
                """);

        var builder = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration);
        var ten = builder.create(createTestReport(2, 0, 8), Metric.TESTS);
        assertThat(ten)
                .hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(8).hasSkippedSize(0).hasPassedSize(2).hasTotalSize(10)
                .hasMaxScore(50)
                .hasImpact(10)
                .hasValue(10);

        assertThat(builder.create(createTestReport(12, 3, 5), Metric.TESTS))
                .hasImpact(35).hasValue(35);
        assertThat(builder.create(createTestReport(95, 5, 0), Metric.TESTS))
                .hasImpact(50).hasValue(50);
        assertThat(builder.create(createTestReport(100, 0, 0), Metric.TESTS))
                .hasImpact(50).hasValue(50);
    }

    @Test
    void shouldScaleRelativeValuesWithLargerMaxScore() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "maxScore": 200,
                    "successRateImpact": 1,
                    "failureRateImpact": 0
                  }
                }
                """);

        var builder = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration);
        var ten = builder.create(createTestReport(2, 3, 5), Metric.TESTS);
        assertThat(ten)
                .hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(200)
                .hasImpact(57)
                .hasValue(57);
    }

    @Test
    void shouldUseRelativeFailureValues() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "maxScore": 100,
                    "failureRateImpact": -1
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration).create(createTestReport(2, 1, 7), Metric.TESTS);
        assertThat(score)
                .hasMaxScore(100)
                .hasImpact(-78)
                .hasValue(22);

        var max = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration).create(createTestReport(1, 0, 0), Metric.TESTS);
        assertThat(max)
                .hasMaxScore(100)
                .hasImpact(0)
                .hasValue(100);
    }

    @Test
    void shouldCalculateImpactAndScoreWithNegativeValues() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "failureRateImpact": -1,
                    "maxScore": 100
                  }
                }
                """);

        var testScore = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration)
                .create(createTestReport(12, 2, 6), Metric.TESTS);
        assertThat(testScore)
                .hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(6).hasSkippedSize(2).hasPassedSize(12).hasTotalSize(20)
                .hasMaxScore(100)
                .hasImpact(-33)
                .hasValue(67);

        assertThat(testScore.toString()).startsWith("{").endsWith("}").containsIgnoringWhitespaces("\"impact\":-33");
    }

    @Test
    void shouldCalculateImpactAndScoreWithPositiveValues() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "successRateImpact": 1,
                    "maxScore": 100
                  }
                }
                """);

        var testScore = new TestScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration)
                .create(createTestReport(12, 2, 6), Metric.TESTS);
        assertThat(testScore)
                .hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(6).hasSkippedSize(2).hasPassedSize(12).hasTotalSize(20)
                .hasMaxScore(100)
                .hasImpact(67)
                .hasValue(67);

        assertThat(testScore.toString()).startsWith("{").endsWith("}").containsIgnoringWhitespaces("\"impact\":67");
    }

    @Test
    void shouldComputePositiveImpactBySizeZero() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "name": "JUnit Test Results",
                    "successRateImpact": 1,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .setConfiguration(configuration)
                .create(createTestReport(0, 0, 0), Metric.TESTS);
        assertThat(score)
                .hasImpact(50)
                .hasValue(50);
    }

    @Test
    void shouldComputeNegativeImpactBySizeZero() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "name": "JUnit Test Results",
                    "successRateImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .setConfiguration(configuration)
                .create(createTestReport(0, 0, 0), Metric.TESTS);
        assertThat(score)
                .hasImpact(-5000)
                .hasValue(0);
    }

    @Test
    void shouldHandleOverflowWithPositiveImpact() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "successRateImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .setConfiguration(configuration)
                .create(createTestReport(10, 20, 0), Metric.TESTS);
        assertThat(score)
                .hasImpact(5000)
                .hasValue(50);
    }

    static ModuleNode createTestReport(final int passed, final int skipped, final int failed) {
        return createTestReport(passed, skipped, failed, StringUtils.EMPTY);
    }

    static ModuleNode createTestReport(final int passed, final int skipped, final int failed, final String prefix) {
        var root = new ModuleNode(String.format(Locale.ENGLISH, "%sTests (%d/%d/%d)", prefix, failed, skipped, passed));
        var tests = new ClassNode("Tests");
        root.addChild(tests);

        for (int i = 0; i < failed; i++) {
            tests.addTestCase(new TestCaseBuilder()
                    .withTestName(prefix + "test-failed-" + i)
                    .withClassName(prefix + "test-class-failed-" + i)
                    .withMessage(prefix + "failed-message-" + i)
                    .withDescription(prefix + "StackTrace-" + i)
                    .withStatus(TestResult.FAILED).build());
        }
        for (int i = 0; i < skipped; i++) {
            tests.addTestCase(new TestCaseBuilder()
                    .withTestName(prefix + "test-skipped-" + i)
                    .withClassName(prefix + "test-class-skipped-" + i)
                    .withStatus(TestResult.SKIPPED).build());
        }
        for (int i = 0; i < passed; i++) {
            tests.addTestCase(new TestCaseBuilder()
                    .withTestName(prefix + "test-passed-" + i)
                    .withStatus(TestResult.PASSED).build());
        }

        var total = passed + failed;
        if (total > 0) {
            root.addValue(new Rate(Metric.TEST_SUCCESS_RATE, passed, total));
        }
        else {
            root.addValue(new Rate(Metric.TEST_SUCCESS_RATE, 0, 1));
        }
        return root;
    }

    @Test
    void shouldHandleOverflowWithNegativeImpact() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "successRateImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .setConfiguration(configuration)
                .create(createTestReport(10, 20, 0), Metric.TESTS);

        assertThat(score)
                .hasImpact(-5000)
                .hasValue(0);
    }

    @Test
    void shouldCreateSubScores() {
        var configuration = createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "successRateImpact": 1,
                    "maxScore": 200
                  }
                }
                """);

        var builder = new TestScoreBuilder()
                .setConfiguration(configuration);
        var first = builder
                .create(createTestReport(8, 0, 2), Metric.TESTS);
        assertThat(first).hasImpact(80 * 2).hasValue(160);
        var second = builder
                .create(createTestReport(2, 6, 8), Metric.TESTS);
        assertThat(second).hasImpact(20 * 2).hasValue(40);

        var aggregation = new TestScoreBuilder()
                .setConfiguration(configuration)
                .setName("Aggregation")
                .aggregate(List.of(first, second));
        assertThat(aggregation)
                .hasImpact(100)
                .hasValue(100)
                .hasName("Aggregation")
                .hasOnlySubScores(first, second);

        var overflow = new TestScoreBuilder()
                .setConfiguration(createConfiguration("""
                {
                  "tests": {
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "successRateImpact": -1,
                    "maxScore": 20
                  }
                }
                """))
                .setName("Aggregation")
                .aggregate(List.of(first, second));
        assertThat(overflow).hasImpact(-10).hasValue(10).hasName("Aggregation");
    }

    private TestConfiguration createConfiguration(final String json) {
        return TestConfiguration.from(json).getFirst();
    }
}
