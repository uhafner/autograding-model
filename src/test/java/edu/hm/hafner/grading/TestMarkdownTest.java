package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.util.FilteredLog;

import static edu.hm.hafner.grading.TestMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TestMarkdown}.
 *
 * @author Ullrich Hafner
 */
class TestMarkdownTest {
    private static final String IMPACT_CONFIGURATION = ":moneybag:|:heavy_minus_sign:|*10*|*-1*|*-5*|:heavy_minus_sign:|:heavy_minus_sign:";
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
        for (int i = 1; i < 1000; i++) {
            classNode.addTestCase(builder.withTestName("Test #" + i).build());
        }
        root.addChild(classNode);

        var score = new AggregatedScore(LOG);
        score.gradeTests(
                new NodeSupplier(t -> root),
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit: 100% successful (:heavy_check_mark: 999 passed)");

        classNode.addTestCase(builder.withTestName("Failed Test").withFailure().build());

        var almost = new AggregatedScore(LOG);
        almost.gradeTests(
                new NodeSupplier(t -> root),
                TestConfiguration.from(configuration));
        assertThat(testMarkdown.createSummary(almost))
                .contains("JUnit: 99% successful (:x: 1 failed, :heavy_check_mark: 999 passed)");
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
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("Tests - 100 of 100")
                .contains("|JUnit|1|0|0|0|0|0")
                .contains(":moneybag:|:heavy_minus_sign:|*-1*|*-2*|*-3*|:heavy_minus_sign:|:heavy_minus_sign:");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 100 of 100: No test results available", ":vertical_traffic_light:");
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
                        "pattern": "**/src/**/TEST*.xml",
                        "icon": "junit.png"
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
        var node = factory.readNode(configurations.get(0).getTools().get(0), new FilteredLog("Errors"));

        score.gradeTests(
                new NodeSupplier(t -> node),
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 35 of 100")
                .contains("|JUnit|3|24|0|13|37|-65")
                .contains("__Aufgabe3Test:shouldSplitToEmptyRight(int)[1]__")
                .containsPattern("```text\\n *Expected size: 3 but was: 5 in:")
                .contains("__edu.hm.hafner.grading.ReportFinderTest:shouldFindTestReports__");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 35 of 100", "65% successful", "13 failed", "24 passed", "junit.png");
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
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 27 of 100")
                .contains("|JUnit|1|5|3|4|12|27")
                .contains(IMPACT_CONFIGURATION);
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 27 of 100", "42% successful", "4 failed", "5 passed", "3 skipped");
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
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 77 of 100",
                        "|Integrationstests|1|5|3|4|12|27",
                        "|Modultests|1|0|0|10|10|-50",
                        IMPACT_CONFIGURATION,
                        "**Total**|**2**|**5**|**3**|**14**|**22**|**-23**",
                        "### Skipped Test Cases",
                        "- test-class-skipped-0#test-skipped-0",
                        "- test-class-skipped-1#test-skipped-1",
                        "- test-class-skipped-2#test-skipped-2");
        assertThat(testMarkdown.createSummary(score)).contains(
                "Integrationstests - 27 of 100: 42% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests - 50 of 100:  0% successful", "10 failed");
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
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit",
                        "|Integrationstests|1|5|3|4|12",
                        "|Modultests|1|0|0|10|10",
                        "**Total**|**2**|**5**|**3**|**14**|**22**",
                        "### Skipped Test Cases",
                        "- test-class-skipped-0#test-skipped-0",
                        "- test-class-skipped-1#test-skipped-1",
                        "- test-class-skipped-2#test-skipped-2")
                .doesNotContain(IMPACT_CONFIGURATION)
                .doesNotContain("Impact");
        assertThat(testMarkdown.createSummary(score)).contains(
                "Integrationstests: 42% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests:  0% successful", "10 failed");
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
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .containsIgnoringWhitespaces(
                        "One - 46 of 100",
                        "|Integrationstests 1|1|5|3|4|12|23",
                        "|Integrationstests 2|1|5|3|4|12|23",
                        "|**Total**|**2**|**10**|**6**|**8**|**24**|**46**",
                        "Two - 40 of 100",
                        "|Modultests 1|1|0|0|10|10|-30",
                        "|Modultests 2|1|0|0|10|10|-30",
                        "|**Total**|**2**|**0**|**0**|**20**|**20**|**-60**",
                        ":moneybag:|:heavy_minus_sign:|*1*|*2*|*3*|:heavy_minus_sign:|:heavy_minus_sign:",
                        ":moneybag:|:heavy_minus_sign:|*-1*|*-2*|*-3*|:heavy_minus_sign:|:heavy_minus_sign:",
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
                "Integrationstests 1 - 23 of 100", "42% successful", "4 failed", "5 passed", "3 skipped",
                "Integrationstests 2 - 23 of 100", "42% successful", "4 failed", "5 passed", "3 skipped",
                "Modultests 1 - 70 of 100", "0% successful", "10 failed",
                "Modultests 2 - 70 of 100", "0% successful", "10 failed");
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
                TestConfiguration.from(configuration));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("StackTrace-50")
                .doesNotContain("StackTrace-100")
                .contains("Too many test failures. Grading output truncated.");
    }
}
