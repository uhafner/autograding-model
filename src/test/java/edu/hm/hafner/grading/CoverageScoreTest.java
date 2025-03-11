package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.grading.CoverageScore.CoverageScoreBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Locale;

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
    private static final String LINE_COVERAGE_NAME = "Line Coverage";
    private static final String BRANCH_COVERAGE_NAME = "Branch Coverage";

    @Test
    void shouldIgnoreEmptyCoverage() {
        var coverageConfiguration = createCoverageConfiguration(-1, 0);
        var rootNode = createReport(Metric.BRANCH, "n/a");
        var coverageScore = new CoverageScoreBuilder()
                .setConfiguration(coverageConfiguration)
                .create(rootNode, Metric.BRANCH);

        assertThat(coverageScore)
                .hasConfiguration(coverageConfiguration)
                .hasMaxScore(100)
                .hasImpact(0)
                .hasMetric(Metric.BRANCH)
                .hasReport(rootNode)
                .hasCoveredPercentage(100)
                .hasMissedPercentage(0);
    }

    @Test
    void shouldCreateInstanceAndGetProperties() {
        var coverageConfiguration = createCoverageConfiguration(1, 1);
        var rootNode = createReport(Metric.LINE, "99/100");
        var coverageScore = new CoverageScoreBuilder()
                .setName(LINE_COVERAGE_NAME)
                .setConfiguration(coverageConfiguration)
                .create(rootNode, Metric.LINE);

        assertThat(coverageScore)
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
    void shouldAssume100PercentIfMissing() {
        var missingCoverage = new CoverageScoreBuilder()
                .setConfiguration(createCoverageConfiguration(1, 1))
                .create(createReport(Metric.LINE, "100/100"), Metric.BRANCH); // wrong metric
        assertThat(missingCoverage).hasMissedPercentage(0).hasCoveredPercentage(100);
    }

    @Test
    void shouldCalculateTotalImpactWithZeroCoveredImpact() {
        var coverageConfiguration = createCoverageConfiguration(-2, 0);
        var coverageScore = new CoverageScoreBuilder()
                .setConfiguration(coverageConfiguration)
                .create(createReport(Metric.LINE, "99/100"), Metric.LINE);

        assertThat(coverageScore).hasImpact(-2);
    }

    @Test
    void shouldCalculateTotalImpactWithZeroMissedImpact() {
        var coverageConfiguration = createCoverageConfiguration(0, 5);
        var coverageScore = new CoverageScoreBuilder()
                .setConfiguration(coverageConfiguration)
                .create(createReport(Metric.LINE, "99/100"), Metric.LINE);

        assertThat(coverageScore).hasImpact(495);
    }

    @Test
    void shouldCalculateTotalImpact() {
        var coverageConfiguration = createCoverageConfiguration(-1, 3);
        var coverageScore = new CoverageScoreBuilder()
                .setConfiguration(coverageConfiguration)
                .create(createReport(Metric.LINE, "99/100"), Metric.LINE);

        assertThat(coverageScore).hasImpact(296).hasValue(100);
    }

    @Test
    void shouldScaleImpactWithMaxScore() {
        var coverageConfiguration = createCoverageConfiguration(0, 1, 50);
        var coverageScore = new CoverageScoreBuilder()
                .setConfiguration(coverageConfiguration)
                .create(createReport(Metric.LINE, "50/100"), Metric.LINE);

        assertThat(coverageScore).hasImpact(25).hasValue(25);
    }

    @Test
    void shouldScaleImpactWithLargerMaxScore() {
        var coverageConfiguration = createCoverageConfiguration(0, 1, 200);
        var coverageScore = new CoverageScoreBuilder()
                .setConfiguration(coverageConfiguration)
                .create(createReport(Metric.LINE, "50/100"), Metric.LINE);

        assertThat(coverageScore).hasImpact(100).hasValue(100);
    }

    @Test
    void shouldSumEmptyResults() {
        var first = new CoverageScoreBuilder()
                .setConfiguration(createCoverageConfiguration(-1, 0))
                .create(createReport(Metric.LINE, "100/100"), Metric.LINE);
        assertThat(first).hasImpact(0).hasValue(100).hasName(LINE_COVERAGE_NAME);
        var second = new CoverageScoreBuilder()
                .setConfiguration(createCoverageConfiguration(-1, 0))
                .create(createReport(Metric.BRANCH, "n/a"), Metric.BRANCH);
        assertThat(second).hasImpact(0).hasValue(100).hasName(BRANCH_COVERAGE_NAME);

        var aggregation = new CoverageScoreBuilder()
                .setName("Aggregation")
                .setConfiguration(createCoverageConfiguration(-1, 0, 100))
                .aggregate(List.of(first, second));
        assertThat(aggregation).hasImpact(0)
                .hasValue(100)
                .hasName("Aggregation")
                .hasOnlySubScores(first, second);
    }

    @Test
    void shouldCreateSubScores() {
        var first = new CoverageScoreBuilder()
                .setConfiguration(createCoverageConfiguration(0, 1))
                .create(createReport(Metric.LINE, "5/100"), Metric.LINE);
        assertThat(first).hasImpact(5).hasValue(5).hasName(LINE_COVERAGE_NAME);
        var second = new CoverageScoreBuilder()
                .setConfiguration(createCoverageConfiguration(0, 1))
                .create(createReport(Metric.BRANCH, "15/100"), Metric.BRANCH);
        assertThat(second).hasImpact(15).hasValue(15).hasName(BRANCH_COVERAGE_NAME);

        var aggregation = new CoverageScoreBuilder()
                .setName("Aggregation")
                .setConfiguration(createCoverageConfiguration(0, 1))
                .aggregate(List.of(first, second));
        assertThat(aggregation).hasImpact(10)
                .hasValue(10)
                .hasName("Aggregation")
                .hasOnlySubScores(first, second);

        var overflow = new CoverageScoreBuilder()
                .setConfiguration(createCoverageConfiguration(0, 20, 100))
                .aggregate(List.of(first, second));
        assertThat(overflow).hasImpact(200).hasValue(100);
    }

    private CoverageConfiguration createCoverageConfiguration(final int missedImpact, final int coveredImpact) {
        return createCoverageConfiguration(missedImpact, coveredImpact, 100);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private CoverageConfiguration createCoverageConfiguration(final int missedImpact, final int coveredImpact,
            final int maxScore) {
        return CoverageConfiguration.from(String.format(Locale.ENGLISH, """
                  {
                      "coverage": {
                        "tools": [
                          {
                            "id": "jacoco",
                            "pattern": "target/jacoco.xml",
                            "metric": "LINE"
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
