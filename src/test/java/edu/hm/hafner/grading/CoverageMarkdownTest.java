package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageMarkdown}.
 *
 * @author Ullrich Hafner
 */
class CoverageMarkdownTest {
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final String IMPACT_CONFIGURATION = "*:moneybag:*|*1*|*-1*|*:ledger:*";

    @Test
    void shouldSkip() {
        var writer = new CoverageMarkdown();

        var markdown = writer.create(new AggregatedScore("{}", LOG));

        assertThat(markdown).contains(CoverageMarkdown.TYPE + ": not enabled");
    }

    @Test
    void shouldShowMaximumScore() {
        var score = new AggregatedScore("""
                {
                  "coverage": {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "JaCoCo",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
                    "metric": "line",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """, LOG);

        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().setMetric(Metric.LINE).setCovered(100).setMissed(0).build());
        score.gradeCoverage((tool, log) -> root);

        var markdown = new CoverageMarkdown().create(score);

        assertThat(markdown).contains("Coverage: 100 of 100")
                .contains("|Root|100|0|100")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var score = new AggregatedScore("""
                {
                  "coverage": {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "JaCoCo",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
                    "metric": "branch",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """, LOG);

        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(60).setMissed(40).build());
        score.gradeCoverage((tool, log) -> root);

        var markdown = new CoverageMarkdown().create(score);

        assertThat(markdown).contains("Coverage: 20 of 100")
                .contains("|Root|60|40|20")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        // TODO: line and branch coverage together or not?
        var score = new AggregatedScore("""
                {
                  "coverage": {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "JaCoCo",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
                    "metric": "branch",
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """, LOG);

        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(60).setMissed(40).build());
        score.gradeCoverage((tool, log) -> root);

        var markdown = new CoverageMarkdown().create(score);

        assertThat(markdown).contains("Coverage: 20 of 100")
                .contains("|Root|60|40|20")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }
    /*
    @Test
    void shouldShowScoreWithTwoResults() {
        var writer = new CoverageMarkdown();

        var score = createScore();
        score.addCoverageScores(new CoverageSupplier() {
            @Override
            private List<CoverageScore> createScores(final CoverageConfiguration configuration) {
                return Arrays.asList(createFirstScore(configuration), createSecondScore(configuration));
            }
        });
        var markdown = writer.create(score);

        assertThat(markdown).contains(TYPE + ": 0 of 100")
                .contains("|First|10|90|-90")
                .contains("|Second|80|20|-20")
                .contains("|**Total**|**45**|**55**|**-110**");
    }

    private CoverageScore createFirstScore(final CoverageConfiguration configuration) {
        return new CoverageScore.CoverageScoreBuilder().withId("First")
                .withDisplayName("First")
                .withConfiguration(configuration)
                .withCoveredPercentage(10)
                .build();
    }

    private CoverageScore createSecondScore(final CoverageConfiguration configuration) {
        return new CoverageScore.CoverageScoreBuilder().withId("Second")
                .withDisplayName("Second")
                .withConfiguration(configuration)
                .withCoveredPercentage(80)
                .build();
    }

    private AggregatedScore createScore() {
        return new AggregatedScore(
                "{\"analysis\":{\"maxScore\":100,\"errorImpact\":-5,\"highImpact\":-3,\"normalImpact\":-2,\"lowImpact\":-1}, \"tests\":{\"maxScore\":100,\"passedImpact\":0,\"failureImpact\":-5,\"skippedImpact\":-1}, \"coverage\":{\"maxScore\":100,\"coveredPercentageImpact\":0,\"missedPercentageImpact\":-1}, \"pit\":{\"maxScore\":100,\"detectedImpact\":0,\"undetectedImpact\":0,\"undetectedPercentageImpact\":-1,\"detectedPercentageImpact\":0}}");
    }
 */
}
