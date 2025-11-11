package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.MethodNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link MetricMarkdown}.
 *
 * @author Ullrich Hafner
 */
class MetricMarkdownTest {
    private static final FilteredLog LOG = new FilteredLog("Test");

    @Test
    void shouldSkipWhenThereAreNoScores() {
        var aggregation = new AggregatedScore(LOG);

        var writer = new MetricMarkdown();

        assertThat(writer.createDetails(aggregation, true)).contains("Metrics Score: not enabled");
        assertThat(writer.createDetails(aggregation)).isEmpty();
        assertThat(writer.createSummary(aggregation)).isEmpty();
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var configuration = """
                {
                  "metrics": [{
                    "name": "Toplevel Metrics",
                    "tools": [
                      {
                        "name": "Cyclomatic Complexity",
                        "id": "metrics",
                        "pattern": "target/**metrics.xml",
                        "metric": "complexity"
                      }
                    ]
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        var root = new ModuleNode("Root");
        var method = new MethodNode("Method", "method");
        root.addChild(method);
        method.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        score.gradeMetrics(
                new NodeSupplier(t -> root),
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).contains(
                "Cyclomatic Complexity: 10", ":cyclone:");
        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|10");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var configuration = """
                {
                  "metrics": [{
                    "name": "Toplevel Metrics",
                    "tools": [
                      {
                        "name": "Cyclomatic Complexity",
                        "id": "metrics",
                        "icon": "custom-icon",
                        "pattern": "target/**metrics.xml",
                        "metric": "complexity"
                      },
                      {
                        "name": "Cognitive Complexity",
                        "id": "metrics",
                        "pattern": "target/**metrics.xml",
                        "metric": "cognitive-complexity"
                      }
                    ]
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeMetrics(
                new NodeSupplier(MetricMarkdownTest::createNodes),
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).contains(
                "Cyclomatic Complexity: 10", "custom-icon",
                "Cognitive Complexity: 100", ":thought_balloon:");

        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|:custom-icon:|Cyclomatic Complexity|10|10|10|10.00|10")
                .contains("|:thought_balloon:|Cognitive Complexity|100|100|100|100.00|100");
    }

    @Test
    void shouldShowScoreWithThreeResults() {
        var configuration = """
                {
                  "metrics": [{
                    "name": "Toplevel Metrics",
                    "tools": [
                      {
                        "name": "Cyclomatic Complexity",
                        "id": "metrics",
                        "pattern": "target/**metrics.xml",
                        "metric": "complexity"
                      },
                      {
                        "name": "Cognitive Complexity",
                        "id": "metrics",
                        "pattern": "target/**metrics.xml",
                        "metric": "cognitive-complexity"
                      },
                      {
                        "name": "LOC",
                        "id": "metrics",
                        "pattern": "target/**metrics.xml",
                        "metric": "loc"
                      }
                    ]
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeMetrics(
                new NodeSupplier(MetricMarkdownTest::createNodes),
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score))
                .contains("Cyclomatic Complexity: 10", "Cognitive Complexity: 100", "LOC: 1000")
                .doesNotContain("Toplevel Metrics");
        assertThat(metricMarkdown.createSummary(score, true)).contains(
                "Toplevel Metrics",
                "Cyclomatic Complexity: 10",
                "Cognitive Complexity: 100",
                "LOC: 1000");

        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|10");
    }

    static ModuleNode createNodes(final ToolConfiguration tool) {
        var root = new ModuleNode(tool.getName());
        var method = new MethodNode("Method", "method");
        root.addChild(method);
        method.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        method.addValue(new Value(Metric.COGNITIVE_COMPLEXITY, 100));
        method.addValue(new Value(Metric.LOC, 1000));
        return root;
    }

    @Test
    void shouldHandleMissingValue() {
        var configuration = """
                {
                  "metrics": [{
                    "name": "Toplevel Metrics",
                    "tools": [
                      {
                        "name": "Cyclomatic Complexity",
                        "id": "metrics",
                        "pattern": "target/**metrics.xml",
                        "metric": "complexity"
                      }
                    ]
                  }]
                }
                """;
        var score = new AggregatedScore(LOG);

        score.gradeMetrics(
                new NodeSupplier(t -> new ModuleNode("Root")),
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).contains(
                "Cyclomatic Complexity: <n/a>");
        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|-|-|-|-|-");
    }

    @Test
    void shouldCreateStatisticsFromRealReport() {
        var configuration = """
                      {
                        "metrics": [
                          {
                            "name": "Toplevel Metrics",
                            "tools": [
                              {
                                "name": "Cyclomatic Complexity",
                                "id": "metrics",
                                "metric": "CYCLOMATIC_COMPLEXITY"
                              },
                              {
                                "name": "Cognitive Complexity",
                                "id": "metrics",
                                "metric": "COGNITIVE_COMPLEXITY"
                              },
                              {
                                "name": "Lines of Code",
                                "id": "metrics",
                                "metric": "LOC"
                              },
                              {
                                "name": "Non Commenting Source Statements",
                                "id": "metrics",
                                "metric": "NCSS"
                              },
                              {
                                "name": "Access to foreign data",
                                "id": "metrics",
                                "metric": "ACCESS_TO_FOREIGN_DATA"
                              },
                              {
                                "name": "Class cohesion",
                                "id": "metrics",
                                "metric": "COHESION"
                              },
                              {
                                "name": "Fan out",
                                "id": "metrics",
                                "metric": "FAN_OUT"
                              },
                              {
                                "name": "Number of accessors",
                                "id": "metrics",
                                "metric": "NUMBER_OF_ACCESSORS"
                              },
                              {
                                "name": "Weight of a class",
                                "id": "metrics",
                                "metric": "WEIGHT_OF_CLASS"
                              },
                              {
                                "name": "Weighted method count",
                                "id": "metrics",
                                "metric": "WEIGHED_METHOD_COUNT"
                              },
                              {
                                "name": "N-Path Complexity",
                                "id": "metrics",
                                "metric": "NPATH_COMPLEXITY"
                              }
                            ]
                          }
                        ]
                      }
                """;
        var score = new AggregatedScore(LOG);
        score.gradeMetrics(
                new NodeSupplier(MetricMarkdownTest::getReadCoverageReport),
                MetricConfiguration.from(configuration));

        var markdown = new MetricMarkdown();

        assertThat(markdown.createSummary(score)).contains(
                "Cyclomatic Complexity: 355",
                "Cognitive Complexity: 172",
                "Lines of Code: 3859",
                "Non Commenting Source Statements: 1199",
                "Access to foreign data: 87",
                "Class cohesion: 71.43%",
                "Fan out: 224",
                "Number of accessors: 14",
                "Weight of a class: 100.00%",
                "Weighted method count: 354",
                "N-Path Complexity: 432");
        assertThat(markdown.createDetails(score))
                .contains(":triangular_ruler:", "Toplevel Metrics",
                        "|Icon|Name|Total|Min|Max|Mean|Median",
                        "|:-:|:-:|:-:|:-:|:-:|:-:|:-:",
                        "|:cyclone:|Cyclomatic Complexity|355|1|8|1.73|1",
                        "|:thought_balloon:|Cognitive Complexity|172|0|11|0.84|0",
                        "|:straight_ruler:|Lines of Code|3859|2|734|148.42|2",
                        "|:memo:|Non Commenting Source Statements|1199|1|162|46.12|1",
                        "|:telescope:|Access to foreign data|87|0|15|3.35|0",
                        "|:link:|Class cohesion|71.43%|0.00%|71.43%|13.59%|0.00%",
                        "|:outbox_tray:|Fan out|224|0|29|8.62|0",
                        "|:calling:|Number of accessors|14|0|2|0.54|0",
                        "|:balance_scale:|Weight of a class|100.00%|0.00%|100.00%|83.65%|0.00%",
                        "|:triangular_ruler:|Weighted method count|354|3|46|14.75|3",
                        "|:loop:|N-Path Complexity|432|1|30|2.11|1"
                );
    }

    private static Node getReadCoverageReport(final ToolConfiguration toolConfiguration) {
        return CoverageMarkdownTest.readCoverageReport("all-metrics.xml", CoverageParserType.METRICS, toolConfiguration);
    }
}
