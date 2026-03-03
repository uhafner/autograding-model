package edu.hm.hafner.grading;

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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static edu.hm.hafner.grading.TestMarkdownTest.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link CoverageMarkdown}.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class CoverageMarkdownTest {
    private static final FilteredLog LOG = new FilteredLog("Test");
    private static final String JACOCO = "jacoco";
    private static final String PIT = "pit";
    private static final String REFERENCE = "reference";
    private static final Optional<Path> NO_DELTA_REPORTS = Optional.empty();

    @Test
    void shouldSkip() {
        var empty = new AggregatedScore(LOG);

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
        var score = new AggregatedScore(LOG);

        var root = new ModuleNode("Root");
        root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(100).withMissed(0).build());
        score.gradeCoverage(new NodeSupplier(t -> root),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);

        var codeCoverageMarkdown = new CodeCoverageMarkdown();
        assertThat(codeCoverageMarkdown.createDetails(score))
                .contains("Code Coverage - 100 of 100", "|JaCoCo|Whole Project|100")
                .doesNotContain("Total");
        assertThat(codeCoverageMarkdown.createSummary(score))
                .contains("JaCoCo (Whole Project) - 100 of 100: 100.00% &mdash; 0 missed lines", ":wavy_dash:");

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
                            "icon": "custom-icon"
                          }
                        ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeCoverage(
                new NodeSupplier(t -> createJacocoReport("Root", 80, 60)),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score))
                .contains("Code Coverage - 20 of 100", "|:custom-icon:|JaCoCo|Whole Project|60")
                .doesNotContain("Total");
        assertThat(codeCoverageMarkdown.createSummary(score))
                .contains("JaCoCo (Whole Project) - 20 of 100: 60.00% &mdash; 40 missed branches", "custom-icon");
        verifyEmptyMutationScore(score);
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
        var score = new AggregatedScore(LOG);

        score.gradeCoverage(
                new NodeSupplier(CoverageMarkdownTest::createTwoReports),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score)).contains(
                "Code Coverage - 40 of 100",
                "|Line Coverage|Whole Project|80.00",
                "|Branch Coverage|Whole Project|60.00",
                "|**Total**|**-**|**70.00**");
        assertThat(codeCoverageMarkdown.createSummary(score)).contains(
                "Line Coverage (Whole Project) - 60 of 100: 80.00% &mdash; 20 missed lines",
                "Branch Coverage (Whole Project) - 20 of 100: 60.00% &mdash; 40 missed branches");
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
        var score = new AggregatedScore(LOG);

        score.gradeCoverage(
                new NodeSupplier(CoverageMarkdownTest::createTwoReports),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score))
                .contains("Code Coverage",
                        "|Line Coverage|Whole Project|80.00",
                        "|Branch Coverage|Whole Project|60.00",
                        "|**Total**|**-**|**70.00**")
                .doesNotContain("Impact");
        assertThat(codeCoverageMarkdown.createSummary(score)).contains(
                "Line Coverage (Whole Project): 80.00% &mdash; 20 missed lines",
                "Branch Coverage (Whole Project): 60.00% &mdash; 40 missed branches");
        verifyEmptyMutationScore(score);
    }

    @Test
    void shouldShowDelta() {
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
        var score = new AggregatedScore(LOG);

        score.gradeCoverage(
                new DeltaNodeSupplier(CoverageMarkdownTest::createReferenceReports),
                CoverageConfiguration.from(configuration), Optional.of(Path.of(REFERENCE)));

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(clean(codeCoverageMarkdown.createDetails(score)))
                .contains("Code Coverage",
                        "|Line Coverage|Whole Project|80.00 (-10.00)",
                        "|Branch Coverage|Whole Project|60.00 (+10.00)",
                        "|**Total**|**-**|**70.00 (±0)**")
                .doesNotContain("Impact");
        assertThat(clean(codeCoverageMarkdown.createSummary(score))).contains(
                "Line Coverage (Whole Project): 80.00% (-10.00)", "20 missed lines",
                "Branch Coverage (Whole Project): 60.00% (+10.00)", "40 missed branches");
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
        var score = new AggregatedScore(LOG);

        score.gradeCoverage(
                new NodeSupplier(CoverageMarkdownTest::createTwoReports),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);

        var codeCoverageMarkdown = new CodeCoverageMarkdown();

        assertThat(codeCoverageMarkdown.createDetails(score)).contains(
                        "JaCoCo - 40 of 100",
                        "|Line Coverage|Whole Project|80.00",
                        "|Branch Coverage|Whole Project|60.00",
                        "|**Total**|**-**|**70.00**")
                .doesNotContain("Mutation Coverage", "PIT");
        assertThat(codeCoverageMarkdown.createSummary(score)).contains(
                "Line Coverage (Whole Project) - 60 of 100: 80.00% &mdash; 20 missed lines",
                "Branch Coverage (Whole Project) - 20 of 100: 60.00% &mdash; 40 missed branches");

        var mutationCoverageMarkdown = new MutationCoverageMarkdown();
        assertThat(mutationCoverageMarkdown.createDetails(score)).contains(
                        "PIT - 40 of 100")
                .doesNotContain("JaCoCo", "Line Coverage", "Branch Coverage");
        assertThat(mutationCoverageMarkdown.createSummary(score)).contains(
                "Mutation Coverage (Whole Project) - 20 of 100: 60.00% &mdash; 40 survived mutations", "pit-black-150x152.png",
                "Test Strength (Whole Project) - 60 of 100: 80.00% &mdash; 20 survived mutations in tested code", ":muscle:");
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
        var score = new AggregatedScore(LOG);
        score.gradeCoverage(
                new NodeSupplier(tool
                        -> readCoverageReport("jacoco-warnings-plugin.xml", CoverageParserType.JACOCO, tool)),
                CoverageConfiguration.from(configuration), NO_DELTA_REPORTS);

        var markdown = new CodeCoverageMarkdown();

        assertThat(markdown.createSummary(score)).contains(
                "Line Coverage (Whole Project): 81.01% &mdash; 1077 missed lines",
                "Branch Coverage (Whole Project): 62.49% &mdash; 446 missed branches");
        assertThat(markdown.createDetails(score))
                .contains("|Icon|Name|Scope|Covered %",
                        "|:-:|:-:|:-:|:-:",
                        "|:wavy_dash:|Line Coverage|Whole Project|81.01",
                        "|:curly_loop:|Branch Coverage|Whole Project|62.49",
                        "|**:heavy_plus_sign:**|**Total**|**-**|**71.75**"
                );
    }

    static Node readCoverageReport(final String fileName, final CoverageParserType parserType, final ToolConfiguration tool) {
        try {
            try (var inputStream = Objects.requireNonNull(CoverageMarkdownTest.class.getResourceAsStream(fileName));
                    var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                var node = new ParserRegistry().get(parserType, ProcessingMode.FAIL_FAST)
                        .parse(reader, fileName, LOG);
                var containerNode = new ContainerNode(tool.getName());
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
            return createJacocoReport(tool.getName(), 80, 60);
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

    private static ModuleNode createJacocoReport(final String name, final int coveredLines, final int coveredBranches) {
        var root = new ModuleNode(name);
        root.addValue(new CoverageBuilder().withMetric(Metric.LINE).withCovered(coveredLines).withMissed(100 - coveredLines).build());
        root.addValue(new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(coveredBranches).withMissed(100 - coveredBranches).build());
        return root;
    }

    static ModuleNode createReferenceReports(final ToolConfiguration tool, final String directory) {
        if (directory.equals(REFERENCE)) {
            return createJacocoReport(REFERENCE + tool.getName(), 90, 50);
        }
        return createJacocoReport(tool.getName(), 80, 60);
    }
}
