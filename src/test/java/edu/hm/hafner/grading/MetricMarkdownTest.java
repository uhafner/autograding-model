package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
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
        root.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        score.gradeMetrics((tool, log) -> root,
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(1).first().asString().contains(
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
                        "icon": "complexity.png",
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

        score.gradeMetrics((tool, log) -> createNodes(tool),
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(2).satisfiesExactly(
                first -> assertThat(first).asString().contains("Cyclomatic Complexity: 10", "complexity.png"),
                second -> assertThat(second).asString().contains("Cognitive Complexity: 100", ":thought_balloon:"));

        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|10");
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

        score.gradeMetrics((tool, log) -> createNodes(tool),
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(3).satisfiesExactly(
                first -> assertThat(first).asString().contains("Cyclomatic Complexity: 10"),
                second -> assertThat(second).asString().contains("Cognitive Complexity: 100"),
                third -> assertThat(third).asString().contains("LOC: 1000"));

        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|10");
    }

    static ModuleNode createNodes(final ToolConfiguration tool) {
        var root = new ModuleNode(tool.getName());
        root.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        root.addValue(new Value(Metric.COGNITIVE_COMPLEXITY, 100));
        root.addValue(new Value(Metric.LOC, 1000));
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

        var root = new ModuleNode("Root");
        score.gradeMetrics((tool, log) -> root,
                MetricConfiguration.from(configuration));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(1).first().asString().contains(
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
        score.gradeMetrics((toolConfiguration, filteredLog) ->
                CoverageMarkdownTest.readCoverageReport(toolConfiguration, filteredLog,
                        "all-metrics.xml", CoverageParserType.METRICS),
                MetricConfiguration.from(configuration));

        var markdown = new MetricMarkdown();

        assertThat(markdown.createSummary(score)).hasSize(11).satisfiesExactly(
                s -> assertThat(s).asString().contains("Cyclomatic Complexity: 355"),
                s -> assertThat(s).asString().contains("Cognitive Complexity: 172"),
                s -> assertThat(s).asString().contains("Lines of Code: 3859"),
                s -> assertThat(s).asString().contains("Non Commenting Source Statements: 1199"),
                s -> assertThat(s).asString().contains("Access to foreign data: 87"),
                s -> assertThat(s).asString().contains("Class cohesion: 71.43%"),
                s -> assertThat(s).asString().contains("Fan out: 224"),
                s -> assertThat(s).asString().contains("Number of accessors: 14"),
                s -> assertThat(s).asString().contains("Weight of a class: 100.00%"),
                s -> assertThat(s).asString().contains("Weighted method count: 354"),
                s -> assertThat(s).asString().contains("N-Path Complexity: 432"));
        assertThat(markdown.createDetails(score))
                .contains(":triangular_ruler:", "Toplevel Metrics",
                        "|Icon|Name|Total|Min|Max|Mean|Median",
                        "|:-:|:-:|:-:|:-:|:-:|:-:|:-:",
                        "|:cyclone:|Cyclomatic Complexity|355|1|8|1.73|1",
                        "|:thought_balloon:|Cognitive Complexity|172|0|11|0.84|0",
                        "|:straight_ruler:|Lines of Code|3859|1|35|6.52|1",
                        "|:memo:|Non Commenting Source Statements|1199|1|21|3.81|1",
                        "|:telescope:|Access to foreign data|87|0|6|0.32|0",
                        "|:link:|Class cohesion|0|0.00%|71.43%|13.59%|0.00%",
                        "|:outbox_tray:|Fan out|224|0|13|1.78|0",
                        "|:calling:|Number of accessors|14|0|2|0.54|0",
                        "|:balance_scale:|Weight of a class|1|0.00%|100.00%|83.65%|0.00%",
                        "|:triangular_ruler:|Weighted method count|354|3|46|14.75|3",
                        "|:loop:|N-Path Complexity|432|1|30|2.11|1"
                );
    }
}
