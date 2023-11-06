package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.AggregatedScore.TestResult;
import edu.hm.hafner.util.FilteredLog;

import static edu.hm.hafner.grading.TestMarkdown.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TestMarkdown}.
 *
 * @author Ullrich Hafner
 */
class TestMarkdownTest {
    private static final String IMPACT_CONFIGURATION = "*:moneybag:*|*10*|*-1*|*-5*|*:ledger:*";
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var writer = new TestMarkdown();

        var markdown = writer.create(new AggregatedScore("{}", LOG));

        assertThat(markdown).contains(TYPE + ": not enabled");
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
        score.gradeTests((tool, log) -> new TestResult(0, 0, 0));

        var markdown = new TestMarkdown().create(score);

        assertThat(markdown)
                .contains("Tests: 100 of 100")
                .contains("|JUnit|0|0|0|0")
                .contains("*:moneybag:*|*-1*|*-2*|*-3*|*:ledger:*")
                .doesNotContain("Total");
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
        score.gradeTests((tool, log) -> createSampleReport());

        var markdown = new TestMarkdown().create(score);
        assertThat(markdown)
                .contains("JUnit: 27 of 100")
                .contains("|JUnit|5|3|4|27")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }

    private TestResult createSampleReport() {
        return new TestResult(5, 4, 3);
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

        var markdown = new TestMarkdown().create(score);

        assertThat(markdown)
                .contains("JUnit: 77 of 100",
                        "|Integrationstests|5|3|4|27",
                        "|Modultests|0|0|10|-50",
                        IMPACT_CONFIGURATION,
                        "**Total**|**5**|**3**|**14**|**-23**");
    }

    private TestResult createTwoReports(final ToolConfiguration tool) {
        if (tool.getId().equals("itest")) {
            return new TestResult(5, 4, 3);
        }
        else if (tool.getId().equals("mtest")) {
            return new TestResult(0, 10, 0);
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

        var markdown = new TestMarkdown().create(score);

        assertThat(markdown)
                .contains(
                        "One: 23 of 100",
                        "|Integrationstests|5|3|4|23",
                        "Two: 70 of 100",
                        "|Modultests|0|0|10|-30",
                        "*:moneybag:*|*1*|*2*|*3*|*:ledger:*",
                        "*:moneybag:*|*-1*|*-2*|*-3*|*:ledger:*")
                .doesNotContain("Total");
    }
}
