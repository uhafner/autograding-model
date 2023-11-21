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
        var empty = new AggregatedScore("{}", LOG);

        assertThat(new CodeCoverageMarkdown().create(empty)).contains(
                "Code Coverage Score: not enabled");
        assertThat(new MutationCoverageMarkdown().create(empty)).contains(
                "Mutation Coverage Score: not enabled");
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
        root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(100).withMissed(0).build());
        score.gradeCoverage((tool, log) -> root);

        assertThat(new CodeCoverageMarkdown().create(score))
                .contains("Code Coverage: 100 of 100", "|JaCoCo|100|0|100", IMPACT_CONFIGURATION)
                .doesNotContain("Total");

        assertThat(new MutationCoverageMarkdown().create(score)).contains(
                "Mutation Coverage Score: not enabled");
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

        var markdown = new CodeCoverageMarkdown().create(score);

        assertThat(markdown)
                .contains("Code Coverage: 20 of 100", "|JaCoCo|60|40|20", IMPACT_CONFIGURATION)
                .doesNotContain("Total");
        assertThat(new MutationCoverageMarkdown().create(score)).contains(
                "Mutation Coverage Score: not enabled");
    }

    static ModuleNode createSampleReport() {
        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(80).withMissed(20).build());
        root.addValue(new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(60).withMissed(40).build());
        return root;
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var score = new AggregatedScore("""
                {
                  "coverage": {
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
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """, LOG);

        score.gradeCoverage((tool, log) -> createTwoReports(tool));

        var markdown = new CodeCoverageMarkdown().create(score);

        assertThat(markdown).contains(
                "Code Coverage: 40 of 100",
                "|Line Coverage|80|20|60",
                "|Branch Coverage|60|40|20",
                "|**Total Ø**|**70**|**30**|**40**",
                IMPACT_CONFIGURATION);
        assertThat(new MutationCoverageMarkdown().create(score)).contains(
                "Mutation Coverage Score: not enabled");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var score = new AggregatedScore("""
                {
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
                """, LOG);

        score.gradeCoverage((tool, log) -> createTwoReports(tool));

        var codeCoverageMarkdown = new CodeCoverageMarkdown().create(score);
        assertThat(codeCoverageMarkdown).contains(
                "JaCoCo: 40 of 100",
                "|Line Coverage|80|20|60",
                "|Branch Coverage|60|40|20",
                "|**Total Ø**|**70**|**30**|**40**",
                IMPACT_CONFIGURATION)
                .doesNotContain("Mutation Coverage", "PIT");

        var mutationCoverageMarkdown = new MutationCoverageMarkdown().create(score);
        assertThat(mutationCoverageMarkdown).contains(
                "PIT: 20 of 100",
                IMPACT_CONFIGURATION)
                .doesNotContain("JaCoCo", "Line Coverage", "Branch Coverage", "Total");
    }

    static ModuleNode createTwoReports(final ToolConfiguration tool) {
        if (tool.getId().equals("jacoco")) {
            var root = new ModuleNode(tool.getDisplayName());
            root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(80).withMissed(20).build());
            root.addValue(new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(60).withMissed(40).build());
            return root;
        }
        else if (tool.getId().equals("pit")) {
            var root = new ModuleNode(tool.getDisplayName());
            root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(90).withMissed(10).build());
            root.addValue(new CoverageBuilder().withMetric(Metric.MUTATION).withCovered(60).withMissed(40).build());
            return root;
        }
        throw new IllegalArgumentException("Unexpected tool ID: " + tool.getId());
    }

}
