package edu.hm.hafner.grading;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class AggregatedScoreTest extends SerializableTest<AggregatedScore> {
    private static final String CONFIGURATION = """
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
                  }],
                  "analysis": [
                    {
                      "name": "Style",
                      "tools": [
                        {
                          "id": "checkstyle",
                          "name": "Checkstyle",
                          "pattern": "target/checkstyle.xml"
                        }
                      ],
                      "errorImpact": 1,
                      "highImpact": 2,
                      "normalImpact": 3,
                      "lowImpact": 4,
                      "maxScore": 100
                    },
                    {
                      "name": "Bugs",
                      "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                      "errorImpact": -11,
                      "highImpact": -12,
                      "normalImpact": -13,
                      "lowImpact": -14,
                      "maxScore": 100
                    }
                  ],
                  "coverage": [
                  {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "Line Coverage",
                            "metric": "line",
                            "pattern": "target/jacoco.xml"
                          },
                          {
                            "id": "jacoco",
                            "name": "Branch Coverage",
                            "metric": "branch",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
                    "name": "JaCoCo",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  },
                  {
                      "tools": [
                          {
                            "id": "pit",
                            "name": "Mutation Coverage",
                            "metric": "mutation",
                            "pattern": "target/pit.xml"
                          }
                        ],
                    "name": "PIT",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """;

    @Override
    protected AggregatedScore createSerializable() {
        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore(CONFIGURATION, logger);

        assertThat(aggregation)
                .hasMaxScore(500)
                .hasAnalysisMaxScore(200)
                .hasTestMaxScore(100)
                .hasCoverageMaxScore(200)
                .hasCodeCoverageMaxScore(100)
                .hasMutationCoverageMaxScore(100)
                .hasAchievedScore(0)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasCodeCoverageAchievedScore(0)
                .hasMutationCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(0)
                .hasRatio(0)
                .hasTestRatio(0)
                .hasCoverageRatio(0)
                .hasCodeCoverageRatio(0)
                .hasMutationCoverageRatio(0)
                .hasAnalysisRatio(0)
                .hasToString("Score: 0 / 500")
                .hasNoAnalysisScores()
                .hasNoTestScores()
                .hasNoCoverageScores()
                .hasNoCodeCoverageScores()
                .hasNoMutationCoverageScores()
                .doesNotHaveTestFailures()
                .doesNotHaveWarnings()
                .hasAnalysis()
                .hasTests()
                .hasCoverage();

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).isEmpty();

        aggregation.gradeAnalysis((tool, log) -> AnalysisMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasAchievedScore(30)
                .hasAnalysis()
                .hasAnalysisRatio(15)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 30 / 500");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 2 static analysis configuration(s)",
                "=> Style Score: 30 of 100",
                "=> Bugs Score: 0 of 100");

        aggregation.gradeTests((tool, log) -> TestMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasAchievedScore(107)
                .hasTestAchievedScore(77)
                .hasTests()
                .hasTestRatio(77)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 107 / 500");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 test configuration(s)",
                "=> JUnit Score: 77 of 100");

        aggregation.gradeCoverage((tool, log) -> CoverageMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasAchievedScore(167)
                .hasTestAchievedScore(77)
                .hasCoverageAchievedScore(60)
                .hasCoverage()
                .hasCoverageRatio(30)
                .hasCodeCoverageAchievedScore(40)
                .hasCodeCoverage()
                .hasCodeCoverageRatio(40)
                .hasMutationCoverageAchievedScore(20)
                .hasMutationCoverage()
                .hasMutationCoverageAchievedScore(20)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 167 / 500");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(String.join("\n", logger.getInfoMessages())).contains(
                "Processing 2 coverage configuration(s)",
                "=> JaCoCo Score: 40 of 100",
                "=> PIT Score: 20 of 100"
        );

        return aggregation;
    }

    @Test
    void shouldHandleEmptyConfiguration() {
        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore("{}", logger);

        assertThat(aggregation)
                .hasMaxScore(0)
                .hasAnalysisMaxScore(0)
                .hasTestMaxScore(0)
                .hasCoverageMaxScore(0)
                .hasAchievedScore(0)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(0)
                .hasRatio(100)
                .hasTestRatio(100)
                .hasCoverageRatio(100)
                .hasAnalysisRatio(100)
                .hasToString("Score: 0 / 0")
                .hasNoAnalysisScores()
                .hasNoTestScores()
                .hasNoCoverageScores()
                .doesNotHaveTestFailures()
                .doesNotHaveWarnings()
                .doesNotHaveAnalysis()
                .doesNotHaveCoverage()
                .doesNotHaveTests();

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).isEmpty();
    }

    @Override
    protected void assertThatRestoredInstanceEqualsOriginalInstance(
            final AggregatedScore original, final AggregatedScore restored) {
        assertThat(restored).usingRecursiveComparison()
                .ignoringFields("log",
                        "analysisScores.report",
                        "analysisScores.subScores.report",
                        "coverageScores.report",
                        "coverageScores.subScores.report",
                        "testScores.report",
                        "testScores.subScores.report")
                .isEqualTo(original);
    }

    public static void main(final String... args) throws IOException {
        new AggregatedScoreTest().createSerializationFile();
    }
}
