package edu.hm.hafner.grading;

import java.io.IOException;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class AggregatedScoreTest extends SerializableTest<AggregatedScore> {
    @Override
    protected AggregatedScore createSerializable() {
        var logger = new FilteredLog("Tests");
        var aggregation = new AggregatedScore("""
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
                      "name": "One",
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
                      "name": "Two",
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
                    "maxScore": 50,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                  ]
                }
                """, logger);

        assertThat(aggregation)
                .hasMaxScore(350)
                .hasAnalysisMaxScore(200)
                .hasTestMaxScore(100)
                .hasCoverageMaxScore(50)
                .hasAchievedScore(0)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(0)
                .hasToString("Score: 0 / 350");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).isEmpty();

        aggregation.gradeAnalysis((tool, log) -> AnalysisMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasMaxScore(350)
                .hasAnalysisMaxScore(200)
                .hasTestMaxScore(100)
                .hasCoverageMaxScore(50)
                .hasAchievedScore(30)
                .hasTestAchievedScore(0)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 30 / 350");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 2 static analysis configuration(s)",
                "=> One Score: 30 of 100",
                "=> Two Score: 0 of 100");

        aggregation.gradeTests((tool, log) -> TestMarkdownTest.createTwoReports(tool));

        assertThat(aggregation)
                .hasMaxScore(350)
                .hasAnalysisMaxScore(200)
                .hasTestMaxScore(100)
                .hasCoverageMaxScore(50)
                .hasAchievedScore(107)
                .hasTestAchievedScore(77)
                .hasCoverageAchievedScore(0)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 107 / 350");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 test configuration(s)",
                "=> JUnit Score: 77 of 100");

        aggregation.gradeCoverage((tool, log) -> CoverageMarkdownTest.createSampleReport());

        assertThat(aggregation)
                .hasMaxScore(350)
                .hasAnalysisMaxScore(200)
                .hasTestMaxScore(100)
                .hasCoverageMaxScore(50)
                .hasAchievedScore(147)
                .hasTestAchievedScore(77)
                .hasCoverageAchievedScore(40)
                .hasAnalysisAchievedScore(30)
                .hasToString("Score: 147 / 350");

        assertThat(logger.getErrorMessages()).isEmpty();
        assertThat(logger.getInfoMessages()).contains(
                "Processing 1 coverage configuration(s)",
                "=> Code Coverage Score: 40 of 50");

        return aggregation;
    }

    @Override
    protected void assertThatRestoredInstanceEqualsOriginalInstance(
            final AggregatedScore original, final AggregatedScore restored) {
        assertThat(restored).usingRecursiveComparison()
                .ignoringFields("log",
                        "analysisScores.report",
                        "analysisScores.subScores.report",
                        "coverageScores.rootNode",
                        "coverageScores.subScores.rootNode")
                .isEqualTo(original);
    }

    public static void main(final String... args) throws IOException {
        new AggregatedScoreTest().createSerializationFile();
    }
}
