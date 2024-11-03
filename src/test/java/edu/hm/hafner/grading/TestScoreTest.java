package edu.hm.hafner.grading;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.grading.TestScore.TestScoreBuilder;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class TestScoreTest {
    private static final String NAME = "Tests";
    private static final String ID = "tests";

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
                .withId(ID)
                .withName(NAME)
                .withConfiguration(configuration);
        var twenty = builder.withReport(createTestReport(2, 3, 5)).build();
        assertThat(twenty)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(100)
                .hasImpact(20)
                .hasValue(20);
        assertThat(twenty.toString()).startsWith("{").endsWith("}")
                .containsIgnoringWhitespaces("\"impact\":20");

        assertThat(builder.withReport(createTestReport(12, 3, 5)).build())
                .hasImpact(60).hasValue(60);
        assertThat(builder.withReport(createTestReport(95, 5, 0)).build())
                .hasImpact(95).hasValue(95);
        assertThat(builder.withReport(createTestReport(100, 0, 0)).build())
                .hasImpact(100).hasValue(100);

        // Check rounding
        assertThat(builder.withReport(createTestReport(197, 0, 3)).build())
                .hasImpact(99).hasValue(99);
        assertThat(builder.withReport(createTestReport(198, 0, 2)).build())
                .hasImpact(99).hasValue(99);
        assertThat(builder.withReport(createTestReport(199, 0, 1)).build())
                .hasImpact(99).hasValue(99);
        assertThat(builder.withReport(createTestReport(200, 0, 0)).build())
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
                .withId(ID)
                .withName(NAME)
                .withConfiguration(configuration);
        var ten = builder.withReport(createTestReport(2, 3, 5)).build();
        assertThat(ten)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(50)
                .hasImpact(10)
                .hasValue(10);

        assertThat(builder.withReport(createTestReport(12, 3, 5)).build())
                .hasImpact(30).hasValue(30);
        assertThat(builder.withReport(createTestReport(95, 5, 0)).build())
                .hasImpact(48).hasValue(48);
        assertThat(builder.withReport(createTestReport(100, 0, 0)).build())
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
                .withId(ID)
                .withName(NAME)
                .withConfiguration(configuration);
        var ten = builder.withReport(createTestReport(2, 3, 5)).build();
        assertThat(ten)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(200)
                .hasImpact(40)
                .hasValue(40);
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

        var builder = new TestScoreBuilder()
                .withId(ID)
                .withName(NAME)
                .withConfiguration(configuration);
        var score = builder.withReport(createTestReport(2, 1, 7)).build();
        assertThat(score)
                .hasMaxScore(100)
                .hasImpact(-70)
                .hasValue(30);
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
                .withReport(createTestReport(2, 3, 5))
                .build();
        assertThat(testScore)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(100)
                .hasImpact(-5 * 5 - 3 * 2 - 2 * 10)
                .hasValue(49);

        assertThat(testScore.toString()).startsWith("{").endsWith("}").containsIgnoringWhitespaces("\"impact\":-51");
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
                .withReport(createTestReport(2, 3, 5))
                .build();
        assertThat(testScore)
                .hasId(ID).hasName(NAME).hasConfiguration(configuration)
                .hasFailedSize(5).hasSkippedSize(3).hasTotalSize(10)
                .hasMaxScore(100)
                .hasImpact(5 * 5 + 3 * 2 + 2 * 10)
                .hasValue(51);

        assertThat(testScore.toString()).startsWith("{").endsWith("}").containsIgnoringWhitespaces("\"impact\":51");
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
                    "passedImpact": 100,
                    "failureImpact": 100,
                    "skippedImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withReport(createTestReport(0, 0, 0))
                .build();
        assertThat(score)
                .hasImpact(0)
                .hasValue(0)
                .hasId(ID)
                .hasName("JUnit Test Results");
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
                    "passedImpact": -100,
                    "failureImpact": -100,
                    "skippedImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withReport(createTestReport(0, 0, 0))
                .build();
        assertThat(score)
                .hasImpact(0)
                .hasValue(50)
                .hasId(ID)
                .hasName("JUnit Test Results");
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
                    "passedImpact": 100,
                    "failureImpact": 100,
                    "skippedImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withReport(createTestReport(0, 20, 10))
                .build();
        assertThat(score)
                .hasImpact(3000)
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
                    "passedImpact": -100,
                    "failureImpact": -100,
                    "skippedImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new TestScoreBuilder()
                .withConfiguration(configuration)
                .withReport(createTestReport(0, 20, 10))
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
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
                    "passedImpact": 3,
                    "failureImpact": -10,
                    "skippedImpact": -1,
                    "maxScore": 200
                  }
                }
                """);

        var builder = new TestScoreBuilder()
                .withConfiguration(configuration);
        var first = builder
                .withReport(createTestReport(3, 4, 3))
                .build();
        assertThat(first).hasImpact(-25).hasValue(175);
        var second = builder
                .withReport(createTestReport(7, 6, 7))
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
                    "tools": [
                        {
                          "id": "tests",
                          "name": "Tests",
                          "pattern": "target/tests.xml"
                        }
                      ],
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
