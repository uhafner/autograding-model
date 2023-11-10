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
                            "metric": "line",
                            "name": "JaCoCo",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
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
                            "metric": "branch",
                            "pattern": "target/jacoco.xml"
                          }
                        ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """, LOG);

        score.gradeCoverage((tool, log) -> createSampleReport());

        var markdown = new CoverageMarkdown().create(score);

        assertThat(markdown).contains("Coverage: 20 of 100")
                .contains("|Root|60|40|20")
                .contains(IMPACT_CONFIGURATION)
                .doesNotContain("Total");
    }

    static ModuleNode createSampleReport() {
        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().setMetric(Metric.LINE).setCovered(80).setMissed(20).build());
        root.addValue(new CoverageBuilder().setMetric(Metric.BRANCH).setCovered(60).setMissed(40).build());
        return root;
    }

    // TODO: line and branch coverage together or not?
}
