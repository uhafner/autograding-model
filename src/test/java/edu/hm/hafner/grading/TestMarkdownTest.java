package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;

import static edu.hm.hafner.grading.TestMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TestMarkdown}.
 *
 * @author Ullrich Hafner
 */
class TestMarkdownTest {
    private static final String IMPACT_CONFIGURATION = ":moneybag:|*10*|*-1*|*-5*|:heavy_minus_sign:|:heavy_minus_sign:";
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var aggregation = new AggregatedScore("{}", LOG);

        var writer = new TestMarkdown();

        assertThat(writer.createDetails(aggregation)).contains(TYPE + ": not enabled");
        assertThat(writer.createSummary(aggregation)).contains(TYPE + ": not enabled");
    }

    @Test
    void shouldShowMaximumScore() {
        var score = new AggregatedScore("""
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
                """, LOG);
        score.gradeTests((tool, log) -> new ModuleNode("Root"));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("Tests - 100 of 100")
                .contains("|JUnit|0|0|0|0|0")
                .contains(":moneybag:|*-1*|*-2*|*-3*|:heavy_minus_sign:|:heavy_minus_sign:");
        assertThat(testMarkdown.createSummary(score))
                .contains("Tests - 100 of 100")
                .contains("0 tests passed");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var score = new AggregatedScore("""
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
                """, LOG);

        score.gradeTests((tool, log) -> TestScoreTest.createTestReport(5, 3, 4));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 27 of 100")
                .contains("|JUnit|5|3|4|12|27")
                .contains(IMPACT_CONFIGURATION);
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 27 of 100", "4 tests failed, 5 passed, 3 skipped");
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var score = new AggregatedScore("""
                {
                  "tests": [{
                    "tools": [
                      {
                        "id": "itest",
                        "name": "Integrationstests",
                        "pattern": "target/i-junit.xml"
                      },
                      {
                        "id": "mtest",
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
                """, LOG);
        score.gradeTests((tool, log) -> createTwoReports(tool));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("JUnit - 77 of 100",
                        "|Integrationstests|5|3|4|12|27",
                        "|Modultests|0|0|10|10|-50",
                        IMPACT_CONFIGURATION,
                        "**Total**|**5**|**3**|**14**|**22**|**-23**");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 77 of 100",
                        "14 tests failed, 5 passed, 3 skipped");
    }

    static Node createTwoReports(final ToolConfiguration tool) {
        if (tool.getId().equals("itest")) {
            return TestScoreTest.createTestReport(5, 3, 4);
        }
        else if (tool.getId().equals("mtest")) {
            return TestScoreTest.createTestReport(0, 0, 10);
        }
        throw new IllegalArgumentException("Unexpected tool ID: " + tool.getId());
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var score = new AggregatedScore("""
                {
                  "tests": [
                  {
                    "name": "One",
                    "tools": [
                      {
                        "id": "itest",
                        "name": "Integrationstests",
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
                        "name": "Modultests",
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
                """, LOG);
        score.gradeTests((tool, log) -> createTwoReports(tool));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .containsIgnoringWhitespaces(
                        "One - 23 of 100",
                        "|Integrationstests|5|3|4|12|23",
                        "Two - 70 of 100",
                        "|Modultests|0|0|10|10|-30",
                        ":moneybag:|*1*|*2*|*3*|:heavy_minus_sign:|:heavy_minus_sign:",
                        ":moneybag:|*-1*|*-2*|*-3*|:heavy_minus_sign:|:heavy_minus_sign:",
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
        assertThat(testMarkdown.createSummary(score))
                .containsIgnoringWhitespaces(
                        "One - 23 of 100",
                        "4 tests failed, 5 passed, 3 skipped",
                        "Two - 70 of 100",
                        "10 tests failed, 0 passed");
    }
}
