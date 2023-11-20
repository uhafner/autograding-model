package edu.hm.hafner.grading;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageScore}.
 *
 * @author Eva-Maria Zeintl
 * @author Ullrich Hafner
 * @author Patrick Rogg
 * @author Johannes Hintermaier
 */
class CoverageScoreTest {
    private static final int PERCENTAGE = 99;
    private static final String LINE_COVERAGE_ID = "line";
    private static final String LINE_COVERAGE_NAME = "Line Coverage";

    @Test
    void shouldCreateInstanceAndGetProperties() {
        var coverageConfiguration = createCoverageConfiguration(1, 1);
        var rootNode = createReport(Metric.LINE, "99/100");
        var coverageScore = new CoverageScore.CoverageScoreBuilder()
                .withId(LINE_COVERAGE_ID)
                .withName(LINE_COVERAGE_NAME)
                .withConfiguration(coverageConfiguration)
                .withReport(rootNode, Metric.LINE)
                .build();

        assertThat(coverageScore)
                .hasId(LINE_COVERAGE_ID)
                .hasName(LINE_COVERAGE_NAME)
                .hasConfiguration(coverageConfiguration)
                .hasMaxScore(100)
                .hasImpact(100)
                .hasMetric(Metric.LINE)
                .hasReport(rootNode)
                .hasCoveredPercentage(PERCENTAGE)
                .hasMissedPercentage(100 - PERCENTAGE);

        assertThat(coverageScore.toString()).startsWith("{").endsWith("}").containsIgnoringWhitespaces("\"impact\":100");
    }

    @Test
    void shouldThrowExceptionWhenMetricIsNotFound() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new CoverageScore.CoverageScoreBuilder()
                        .withConfiguration(createCoverageConfiguration(1, 1))
                        .withReport(createReport(Metric.LINE, "99/100"), Metric.BRANCH)
                        .build()
        );
    }

    @Test
    void shouldCalculateTotalImpactWithZeroCoveredImpact() {
        var coverageConfiguration = createCoverageConfiguration(-2, 0);
        var coverageScore = new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(coverageConfiguration)
                .withReport(createReport(Metric.LINE, "99/100"), Metric.LINE)
                .build();

        assertThat(coverageScore).hasImpact(-2);
    }

    @Test
    void shouldCalculateTotalImpactWithZeroMissedImpact() {
        var coverageConfiguration = createCoverageConfiguration(0, 5);
        var coverageScore = new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(coverageConfiguration)
                .withReport(createReport(Metric.LINE, "99/100"), Metric.LINE)
                .build();

        assertThat(coverageScore).hasImpact(495);
    }

    @Test
    void shouldCalculateTotalImpact() {
        var coverageConfiguration = createCoverageConfiguration(-1, 3);
        var coverageScore = new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(coverageConfiguration)
                .withReport(createReport(Metric.LINE, "99/100"), Metric.LINE)
                .build();

        assertThat(coverageScore).hasImpact(296).hasValue(100);
    }

    @Test
    void shouldCreateSubScores() {
        var first = new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(createCoverageConfiguration(0, 1))
                .withReport(createReport(Metric.LINE, "5/100"), Metric.LINE)
                .build();
        assertThat(first).hasImpact(5).hasValue(5).hasId("coverage").hasName("Code Coverage");
        var second = new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(createCoverageConfiguration(0, 1))
                .withReport(createReport(Metric.BRANCH, "15/100"), Metric.BRANCH)
                .build();
        assertThat(second).hasImpact(15).hasValue(15).hasId("coverage").hasName("Code Coverage");

        var aggregation = new CoverageScore.CoverageScoreBuilder()
                .withId("aggregation")
                .withName("Aggregation")
                .withConfiguration(createCoverageConfiguration(0, 1))
                .withScores(List.of(first, second))
                .build();
        assertThat(aggregation).hasImpact(10)
                .hasValue(10)
                .hasId("aggregation")
                .hasName("Aggregation")
                .hasOnlySubScores(first, second);

        var overflow = new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(createCoverageConfiguration(0, 1, 5))
                .withScores(List.of(first, second))
                .build();
        assertThat(overflow).hasImpact(10).hasValue(5);
    }

    private CoverageConfiguration createCoverageConfiguration(final int missedImpact, final int coveredImpact) {
        return createCoverageConfiguration(missedImpact, coveredImpact, 100);
    }

    private CoverageConfiguration createCoverageConfiguration(final int missedImpact, final int coveredImpact,
            final int maxScore) {
        return CoverageConfiguration.from(String.format("""
                  {
                      "coverage": {
                        "tools": [
                          {
                            "id": "jacoco",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
                        "maxScore": %d,
                        "coveredPercentageImpact": %d,
                        "missedPercentageImpact": %d,
                        "metric": "LINE"
                      }
                  }
                """, maxScore, coveredImpact, missedImpact)).get(0);
    }

    private ModuleNode createReport(final Metric metric, final String coverageRepresentation) {
        var empty = new ModuleNode("Coverage " + coverageRepresentation);
        var coverage = Coverage.valueOf(metric, coverageRepresentation);
        empty.addValue(coverage);
        return empty;
    }
}
