package edu.hm.hafner.grading;

import edu.hm.hafner.coverage.*;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.util.FilteredLog;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static edu.hm.hafner.grading.TestMarkdown.JUNIT_ICON;
import static edu.hm.hafner.grading.TestMarkdown.TYPE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the class {@link TestMarkdown}.
 *
 * @author Ullrich Hafner
 */
class TestMarkdownTest {
    private static final String IMPACT_CONFIGURATION = ":moneybag:|:heavy_minus_sign:|:heavy_minus_sign:|:heavy_minus_sign:|*10*|*-1*|*-5*|:heavy_minus_sign:|:heavy_minus_sign:|:heavy_minus_sign:";
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final int TOO_MANY_FAILURES = 400;

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var aggregation = new AggregatedScore(LOG);

        var writer = new TestMarkdown();

        assertThat(writer.createDetails(aggregation)).isEmpty();
        assertThat(writer.createDetails(aggregation, true)).contains(TYPE + ": not enabled");
        assertThat(writer.createSummary(aggregation)).isEmpty();
    }

    @Test
    void shouldNeverShow100PercentOnFailures() {
        var configuration = """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "target/junit.xml"
                      }
                    ]
                  }
                }
                """;

        var root = new ModuleNode("module");
        var classNode = new ClassNode("class");
        var builder = new TestCaseBuilder();
        for (int i = 1; i < 1_000_000; i++) {
            classNode.addTestCase(builder.withTestName("Test #" + i).build());
        }
        root.addChild(classNode);
        root.addValue(new Rate(Metric.TEST_SUCCESS_RATE, 999_999, 999_999));

        var score = new AggregatedScore(LOG);
        score.gradeTests(
                new NodeSupplier(t -> root),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit (Whole Project): 100.00% successful (999999 passed)");

        classNode.addTestCase(builder.withTestName("Failed Test").withFailure().build());
        root.replaceValue(new Rate(Metric.TEST_SUCCESS_RATE, 999_999, 1_000_000));

        var almost = new AggregatedScore(LOG);
        almost.gradeTests(
                new NodeSupplier(t -> root),
                TestConfiguration.from(configuration), Optional.empty());
        assertThat(testMarkdown.createSummary(almost))
                .contains("JUnit (Whole Project): 99.99% successful (1 failed, 999999 passed)");
    }

    @Test
    void shouldShowMaximumScore() {
        var configuration = """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "passedImpact": -1,
                    "skippedImpact": -2,
                    "failureImpact": -3,
                    "maxScore": 100
                  }
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeTests(
                new NodeSupplier(t -> new ModuleNode("Root")),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("Tests - 100 of 100")
                .contains(JUNIT_ICON + "|JUnit|Whole Project|0|0|0|0|0|:white_check_mark:");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit (Whole Project) - 100 of 100: No test results available", JUNIT_ICON);
    }

    @Test
    void shouldShowScoreWithRealResult() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "**/src/**/grading/TEST*.xml",
                        "icon": "custom-icon"
                      }
                    ],
                    "name": "JUnit",
                    "passedImpact": 0,
                    "skippedImpact": -1,
                    "failureImpact": -5,
                    "maxScore": 100
                  }]
                }
                """;
        var configurations = TestConfiguration.from(configuration);
        var score = new AggregatedScore(LOG);

        var factory = new FileSystemToolParser();
        var node = factory.readNode(configurations.getFirst().getTools().getFirst(), ".", new FilteredLog("Errors"));

        score.gradeTests(
                new NodeSupplier(t -> node),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 35 of 100")
                .contains("|:custom-icon:|JUnit|Whole Project|37|24|0|13|-65|:x:")
                .contains("__Aufgabe3Test:shouldSplitToEmptyRight(int)[1]__")
                .containsPattern("```text\\n *Expected size: 3 but was: 5 in:")
                .contains("__edu.hm.hafner.grading.ReportFinderTest:shouldFindTestReports__");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit (Whole Project) - 35 of 100", "64.86% successful", "13 failed", "24 passed", "custom-icon");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "name": "JUnit",
                    "passedImpact": 10,
                    "skippedImpact": -1,
                    "failureImpact": -5,
                    "maxScore": 100
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeTests(
                new NodeSupplier(t -> TestScoreTest.createTestReport(5, 3, 4)),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 27 of 100")
                .contains("|JUnit|Whole Project|12|5|3|4|27|:x:");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit (Whole Project) - 27 of 100", "56% successful", "4 failed", "5 passed", "3 skipped");
    }

    @Test
    void shouldShowScoreWithNoFailures() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "name": "JUnit",
                    "failureRateImpact": -1,
                    "maxScore": 100
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeTests(
                new NodeSupplier(t -> TestScoreTest.createTestReport(23, 0, 0)),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 100 of 100")
                .contains("|JUnit|Whole Project|23|100|0|:white_check_mark:");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit (Whole Project) - 100 of 100", "100.00% successful", "23 passed");
        assertThat(score.getAchievedScore()).isEqualTo(100);
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "Integrationstests",
                        "pattern": "target/i-junit.xml"
                      },
                      {
                        "id": "junit",
                        "name": "Modultests",
                        "pattern": "target/u-junit.xml"
                      }
                    ],
                    "name": "JUnit",
                    "passedImpact": 10,
                    "skippedImpact": -1,
                    "failureImpact": -5,
                    "maxScore": 100
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);
        score.gradeTests(
                new NodeSupplier(TestMarkdownTest::createTwoReports),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 77 of 100",
                        "|Integrationstests|Whole Project|12|5|3|4|27|:x:",
                        "|Modultests|Whole Project|10|0|0|10|-50|:x:",
                        "**Total**|**-**|**-**|**22**|**5**|**3**|**14**|**-23**|**-**",
                        "### Skipped Tests",
                        "- test-class-skipped-0#test-skipped-0",
                        "- test-class-skipped-1#test-skipped-1",
                        "- test-class-skipped-2#test-skipped-2");
        assertThat(testMarkdown.createSummary(score)).contains(
                "Integrationstests (Whole Project) - 27 of 100: 55.56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests (Whole Project) - 50 of 100: 0.00% successful", "10 failed");
    }

    @Test
    void shouldShowNoImpactsWithTwoSubResults() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "Integrationstests",
                        "pattern": "target/i-junit.xml"
                      },
                      {
                        "id": "junit",
                        "name": "Modultests",
                        "pattern": "target/u-junit.xml"
                      }
                    ],
                    "name": "JUnit"
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);
        score.gradeTests(
                new NodeSupplier(TestMarkdownTest::createTwoReports),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit",
                        "|Integrationstests|Whole Project|12|5|3|4|:x:",
                        "|Modultests|Whole Project|10|0|0|10|:x:",
                        "**Total**|**-**|**-**|**22**|**5**|**3**|**14**|**-**",
                        "### Skipped Tests",
                        "- test-class-skipped-0#test-skipped-0",
                        "- test-class-skipped-1#test-skipped-1",
                        "- test-class-skipped-2#test-skipped-2")
                .doesNotContain(IMPACT_CONFIGURATION)
                .doesNotContain("Impact");
        assertThat(testMarkdown.createSummary(score)).contains(
                "Integrationstests (Whole Project): 55.56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests (Whole Project): 0.00% successful", "10 failed");
    }

    static Node createTwoReports(final ToolConfiguration tool) {
        if (tool.getName().startsWith("Integrationstests")) {
            if (tool.getName().contains("2")) {
                return TestScoreTest.createTestReport(5, 3, 4, "2nd-");
            }
            return TestScoreTest.createTestReport(5, 3, 4);
        }
        else if (tool.getName().startsWith("Modultests")) {
            if (tool.getName().contains("2")) {
                return TestScoreTest.createTestReport(0, 0, 10, "2nd-");
            }
            return TestScoreTest.createTestReport(0, 0, 10);
        }
        throw new IllegalArgumentException("Unexpected tool: " + tool.getName());
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var configuration = """
                {
                  "tests": [
                  {
                    "name": "One",
                    "tools": [
                      {
                        "id": "itest",
                        "name": "Integrationstests 1",
                        "pattern": "target/i-junit.xml"
                      },
                      {
                        "id": "itest",
                        "name": "Integrationstests 2",
                        "pattern": "target/i-junit.xml"
                      }
                    ],
                    "passedImpact": 1,
                    "skippedImpact": 2,
                    "failureImpact": 3,
                    "maxScore": 100
                  },
                  {
                    "name": "Two",
                    "tools": [
                      {
                        "id": "mtest",
                        "name": "Modultests 1",
                        "pattern": "target/m-junit.xml"
                      },
                      {
                        "id": "mtest",
                        "name": "Modultests 2",
                        "pattern": "target/m-junit.xml"
                      }
                    ],
                    "passedImpact": -1,
                    "skippedImpact": -2,
                    "failureImpact": -3,
                    "maxScore": 100
                  }
                  ]
                }
                """;
        var score = new AggregatedScore(LOG);
        score.gradeTests(
                new NodeSupplier(TestMarkdownTest::createTwoReports),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .containsIgnoringWhitespaces(
                        "One - 46 of 100",
                        "|Integrationstests 1|Whole Project|12|5|3|4|23|:x:",
                        "|Integrationstests 2|Whole Project|12|5|3|4|23|:x:",
                        "|**Total**|**-**|**-**|**24**|**10**|**6**|**8**|**46**|**-**",
                        "Two - 40 of 100",
                        "|Modultests 1|Whole Project|10|0|0|10|-30|:x:",
                        "|Modultests 2|Whole Project|10|0|0|10|-30|:x:",
                        "|**Total**|**-**|**-**|**20**|**0**|**0**|**20**|**-60**|**-**",
                        "__test-class-failed-0:test-failed-0__",
                        "__test-class-failed-1:test-failed-1__",
                        "__test-class-failed-2:test-failed-2__",
                        "failed-message-0",
                        "failed-message-1",
                        "failed-message-2",
                        "<summary>Stack Trace</summary>",
                        "```text StackTrace-0```",
                        "```text StackTrace-1```",
                        "```text StackTrace-2```");
        assertThat(testMarkdown.createSummary(score)).contains(
                "Integrationstests 1 (Whole Project) - 23 of 100", "56% successful", "4 failed", "5 passed", "3 skipped",
                "Integrationstests 2 (Whole Project) - 23 of 100", "56% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests 1 (Whole Project) - 70 of 100", "0% successful", "10 failed",
                "Modultests 2 (Whole Project) - 70 of 100", "0% successful", "10 failed");
    }

    @Test
    void shouldTruncateFailures() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "name": "JUnit",
                    "failureImpact": -1,
                    "maxScore": 100
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeTests(
                new NodeSupplier(t -> TestScoreTest.createTestReport(0, 0, TOO_MANY_FAILURES)),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("Too many test failures. Grading output truncated.",
                        "- test-class-failed-250#test-failed-250")
                .doesNotContain("StackTrace", "- test-class-failed-250#test-failed-300");
    }

    @Test
    void shouldTruncateSingleElementOnly() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit-Truncated",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "name": "JUnit-Truncated",
                    "failureImpact": -1,
                    "maxScore": 100
                  },
                  {
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit-Not-Truncated",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "name": "JUnit-Not-Truncated",
                    "failureImpact": -1,
                    "maxScore": 100
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeTests(
                new NodeSupplier(t -> TestScoreTest.createTestReport(0, 0, TOO_MANY_FAILURES)),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("Too many test failures. Grading output truncated.",
                        "- test-class-failed-125#test-failed-125")
                .doesNotContain("StackTrace", "- test-class-failed-130#test-failed-130")
                .contains("JUnit-Truncated - 0 of 100",
                        "JUnit-Not-Truncated - 0 of 100");
    }

    @Test
    void shouldShowOpenMoji() {
        var configuration = """
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit",
                        "pattern": "target/junit.xml"
                      }
                    ],
                    "name": "JUnit",
                    "icon": "openmoji:1F6AB",
                    "failureImpact": -1,
                    "maxScore": 100
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeTests(
                new NodeSupplier(t -> TestScoreTest.createTestReport(1, 0, 0)),
                TestConfiguration.from(configuration), Optional.empty());

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("## <img src=\"https://openmoji.org/data/color/svg/1F6AB.svg\"")
                .contains("|<img src=\"https://openmoji.org/data/color/svg/1F6AB.svg\"");
    }
}
