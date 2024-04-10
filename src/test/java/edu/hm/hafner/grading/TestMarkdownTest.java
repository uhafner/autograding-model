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
    private static final String IMPACT_CONFIGURATION = ":moneybag:|:heavy_minus_sign:|*10*|*-1*|*-5*|:heavy_minus_sign:|:heavy_minus_sign:";
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final int TOO_MANY_FAILURES = 400;

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
                .contains("|JUnit|1|0|0|0|0|0")
                .contains(":moneybag:|:heavy_minus_sign:|*-1*|*-2*|*-3*|:heavy_minus_sign:|:heavy_minus_sign:");
        assertThat(testMarkdown.createSummary(score))
                .endsWith("Tests - 100 of 100");
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
                .contains("|JUnit|1|5|3|4|12|27")
                .contains(IMPACT_CONFIGURATION);
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 27 of 100", "42 % successful", "4 failed", "5 passed", "3 skipped");
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
                        "|Integrationstests|1|5|3|4|12|27",
                        "|Modultests|1|0|0|10|10|-50",
                        IMPACT_CONFIGURATION,
                        "**Total**|**2**|**5**|**3**|**14**|**22**|**-23**",
                        "### Skipped Test Cases",
                        "- test-class-skipped-0#test-skipped-0",
                        "- test-class-skipped-1#test-skipped-1",
                        "- test-class-skipped-2#test-skipped-2");
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit - 77 of 100", "23 % successful", "14 failed", "5 passed", "3 skipped");
    }

    @Test
    void shouldShowNoImpactsWithTwoSubResults() {
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
                    "name": "JUnit"
                  }]
                }
                """, LOG);
        score.gradeTests((tool, log) -> createTwoReports(tool));

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
        assertThat(testMarkdown.createSummary(score))
                .contains("JUnit", "23 %", "14 failed", "5 passed", "3 skipped");
    }

    static Node createTwoReports(final ToolConfiguration tool) {
        if (tool.getId().equals("itest")) {
            if (tool.getName().contains("2")) {
                return TestScoreTest.createTestReport(5, 3, 4, "2nd-");
            }
            return TestScoreTest.createTestReport(5, 3, 4);
        }
        else if (tool.getId().equals("mtest")) {
            if (tool.getName().contains("2")) {
                return TestScoreTest.createTestReport(0, 0, 10, "2nd-");
            }
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
                """, LOG);
        score.gradeTests((tool, log) -> createTwoReports(tool));

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
        assertThat(testMarkdown.createSummary(score))
                .containsIgnoringWhitespaces(
                        "One - 46 of 100", "42 % successful",
                        "8 failed", "10 passed", "6 skipped",
                        "Two - 40 of 100", "0 % successful",
                        "20 failed");
    }

    @Test
    void shouldTruncateFailures() {
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
                    "failureImpact": -1,
                    "maxScore": 100
                  }]
                }
                """, LOG);

        score.gradeTests((tool, log) -> TestScoreTest.createTestReport(0, 0, TOO_MANY_FAILURES));

        var testMarkdown = new TestMarkdown();

        assertThat(testMarkdown.createDetails(score))
                .contains("StackTrace-50")
                .doesNotContain("StackTrace-100")
                .contains("Too many test failures. Grading output truncated.");
    }
}
