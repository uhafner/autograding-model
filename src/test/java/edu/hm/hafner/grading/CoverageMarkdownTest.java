package edu.hm.hafner.grading;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.registry.ParserRegistry;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageMarkdown}.
 *
 * @author Ullrich Hafner
 */
class CoverageMarkdownTest {
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final String IMPACT_CONFIGURATION = ":moneybag:|*1*|*-1*|:heavy_minus_sign:";
    private static final String JACOCO = "jacoco";
    private static final String PIT = "pit";

    @Test
    void shouldSkip() {
        var empty = new AggregatedScore("{}", LOG);

        var codeCoverageMarkdown = new CodeCoverageMarkdown();
        assertThat(codeCoverageMarkdown.createDetails(empty, true)).contains(
                "Code Coverage Score: not enabled");
        assertThat(codeCoverageMarkdown.createDetails(empty)).isEmpty();
        assertThat(codeCoverageMarkdown.createSummary(empty)).isEmpty();
        var mutationCoverageMarkdown = new MutationCoverageMarkdown();
        assertThat(mutationCoverageMarkdown.createDetails(empty, true)).contains(
                "Mutation Coverage Score: not enabled");
        assertThat(mutationCoverageMarkdown.createDetails(empty)).isEmpty();
        assertThat(mutationCoverageMarkdown.createSummary(empty)).isEmpty();
    }

    @Test
    void shouldShowMaximumScore() {
        var configuration = """
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
                """;
        var score = new AggregatedScore(configuration, LOG);

        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(100).withMissed(0).build());
        score.gradeCoverage((tool, log) -> root,
                CoverageConfiguration.from(configuration));

        var codeCoverageMarkdown = new CodeCoverageMarkdown();
        assertThat(codeCoverageMarkdown.createDetails(score))
                .contains("Code Coverage - 100 of 100", "|JaCoCo|100|0|100", IMPACT_CONFIGURATION)
                .doesNotContain("Total");
        assertThat(codeCoverageMarkdown.createSummary(score)).hasSize(1).first().asString()
                .contains("JaCoCo - 100 of 100: 100% (0 missed lines)", ":wavy_dash:");

        verifyEmptyMutationScore(score);
    }

    private void verifyEmptyMutationScore(final AggregatedScore score) {
        assertThat(new MutationCoverageMarkdown().createDetails(score)).isEmpty();
        assertThat(new MutationCoverageMarkdown().createDetails(score, true)).contains(
                "Mutation Coverage Score: not enabled");
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var configuration = """
                {
                  "coverage": {
                      "tools": [
                          {
                            "id": "jacoco",
                            "name": "JaCoCo",
                            "metric": "branch",
                            "pattern": "target/jacoco.xml",
                            "icon": "jacoco.png"
                          }
                        ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """;
        var score = new AggregatedScore(configuration, LOG);

        score.gradeCoverage((tool, log) -> createSampleReport(),
                CoverageConfiguration.from(configuration));

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score))
                .contains("Code Coverage - 20 of 100", "|JaCoCo|60|40|20", IMPACT_CONFIGURATION)
                .doesNotContain("Total");
        assertThat(codeCoverageMarkdown.createSummary(score)).hasSize(1).first().asString()
                .contains("JaCoCo - 20 of 100: 60% (40 missed branches)", "jacoco.png");
        verifyEmptyMutationScore(score);
    }

    static ModuleNode createSampleReport() {
        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(80).withMissed(20).build());
        root.addValue(new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(60).withMissed(40).build());
        return root;
    }

    @Test
    void shouldShowScoreWithTwoSubResults() {
        var configuration = """
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
                """;
        var score = new AggregatedScore(configuration, LOG);

        score.gradeCoverage((tool, log) -> createTwoReports(tool),
                CoverageConfiguration.from(configuration));

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score)).contains(
                "Code Coverage - 40 of 100",
                "|Line Coverage|80|20|60",
                "|Branch Coverage|60|40|20",
                "|**Total Ø**|**70**|**30**|**40**",
                IMPACT_CONFIGURATION);
        assertThat(codeCoverageMarkdown.createSummary(score)).hasSize(2).satisfiesExactly(
                summary -> assertThat(summary).contains("Line Coverage - 60 of 100: 80% (20 missed lines)"),
                summary -> assertThat(summary).contains("Branch Coverage - 20 of 100: 60% (40 missed branches)"));
        verifyEmptyMutationScore(score);
    }

    @Test
    void shouldShowNoImpactsWithTwoSubResults() {
        var configuration = """
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
                        ]
                  }
                }
                """;
        var score = new AggregatedScore(configuration, LOG);

        score.gradeCoverage((tool, log) -> createTwoReports(tool),
                CoverageConfiguration.from(configuration));

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score))
                .contains("Code Coverage",
                        "|Line Coverage|80|20",
                        "|Branch Coverage|60|40",
                        "|**Total Ø**|**70**|**30**")
                .doesNotContain(IMPACT_CONFIGURATION)
                .doesNotContain("Impact");
        assertThat(codeCoverageMarkdown.createSummary(score)).hasSize(2)
                .satisfiesExactly(
                        summary -> assertThat(summary).contains("Line Coverage: 80% (20 missed lines)"),
                        summary -> assertThat(summary).contains("Branch Coverage: 60% (40 missed branches)"));
        verifyEmptyMutationScore(score);
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var configuration = """
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
                          },
                            {
                                "id": "pit",
                                "name": "Test Strength",
                                "metric": "test-strength",
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
        var score = new AggregatedScore(configuration, LOG);

        score.gradeCoverage((tool, log) -> createTwoReports(tool),
                CoverageConfiguration.from(configuration));

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score)).contains(
                        "JaCoCo - 40 of 100",
                        "|Line Coverage|80|20|60",
                        "|Branch Coverage|60|40|20",
                        "|**Total Ø**|**70**|**30**|**40**",
                        IMPACT_CONFIGURATION)
                .doesNotContain("Mutation Coverage", "PIT");
        assertThat(codeCoverageMarkdown.createSummary(score)).hasSize(2)
                .satisfiesExactly(
                        summary -> assertThat(summary).contains("Line Coverage - 60 of 100: 80% (20 missed lines)"),
                        summary -> assertThat(summary).contains(
                                "Branch Coverage - 20 of 100: 60% (40 missed branches)"));

        var mutationCoverageMarkdown = new MutationCoverageMarkdown();
        assertThat(mutationCoverageMarkdown.createDetails(score)).contains(
                        "PIT - 40 of 100", IMPACT_CONFIGURATION)
                .doesNotContain("JaCoCo", "Line Coverage", "Branch Coverage");
        assertThat(mutationCoverageMarkdown.createSummary(score)).hasSize(2).satisfiesExactly(
                first -> assertThat(first)
                        .contains("Mutation Coverage - 20 of 100: 60% (40 survived mutations)",
                                "pit-black-150x152.png"),
                second -> assertThat(second)
                        .contains("Test Strength - 60 of 100: 80% (20 survived mutations in tested code)",
                                ":muscle:"));
    }

    @Test
    void shouldCreateStatisticsFromRealReport() {
        var configuration = """
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
                        ]
                  }
                }
                """;
        var score = new AggregatedScore(configuration, LOG);
        score.gradeCoverage((toolConfiguration, filteredLog) ->
                readCoverageReport(toolConfiguration, filteredLog, "jacoco-warnings-plugin.xml", CoverageParserType.JACOCO),
                CoverageConfiguration.from(configuration));

        var markdown = new CodeCoverageMarkdown();

        assertThat(markdown.createSummary(score)).hasSize(2).satisfiesExactly(
                s -> assertThat(s).asString().contains("Line Coverage: 81% (1077 missed lines)"),
                s -> assertThat(s).asString().contains("Branch Coverage: 62% (446 missed branches)"));
        assertThat(markdown.createDetails(score))
                .contains("|Icon|Name|Covered %|Missed %",
                        "|:-:|:-:|:-:|:-:",
                        "|:wavy_dash:|Line Coverage|81|19",
                        "|:curly_loop:|Branch Coverage|62|38",
                        "|**:heavy_plus_sign:**|**Total Ø**|**71**|**29**"
                );
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    static Node readCoverageReport(final ToolConfiguration toolConfiguration, final FilteredLog filteredLog,
            final String fileName, final CoverageParserType parserType) {
        try {
            try (var inputStream = Objects.requireNonNull(CoverageMarkdownTest.class.getResourceAsStream(fileName));
                    var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                var node = new ParserRegistry().get(parserType, ProcessingMode.FAIL_FAST)
                        .parse(reader, fileName, LOG);
                var containerNode = new ContainerNode(toolConfiguration.getName());
                containerNode.addChild(node);
                return containerNode;
            }
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    static ModuleNode createTwoReports(final ToolConfiguration tool) {
        if (JACOCO.equals(tool.getId())) {
            var root = new ModuleNode(tool.getName());
            root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(80).withMissed(20).build());
            root.addValue(new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(60).withMissed(40).build());
            return root;
        }
        else if (PIT.equals(tool.getId())) {
            var root = new ModuleNode(tool.getName());
            root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(90).withMissed(10).build());
            root.addValue(new CoverageBuilder().withMetric(Metric.MUTATION).withCovered(60).withMissed(40).build());
            root.addValue(
                    new CoverageBuilder().withMetric(Metric.TEST_STRENGTH).withCovered(80).withMissed(20).build());
            return root;
        }
        throw new IllegalArgumentException("Unexpected tool: " + tool.getName());
    }
}
