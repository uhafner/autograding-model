package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Value;
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
        var aggregation = new AggregatedScore("{}", LOG);

        var writer = new MetricMarkdown();

        assertThat(writer.createDetails(aggregation, true)).contains("Metrics Score: not enabled");
        assertThat(writer.createDetails(aggregation)).isEmpty();
        assertThat(writer.createSummary(aggregation)).isEmpty();
    }

    @Test
    void shouldShowScoreWithOneResult() {
        var score = new AggregatedScore("""
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
                """, LOG);

        var root = new ModuleNode("Root");
        root.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        score.gradeMetrics((tool, log) -> root);

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(1).first().asString().contains(
                "Cyclomatic Complexity: 10", ":cyclone:");
        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|10");
    }

    @Test
    void shouldShowScoreWithTwoResults() {
        var score = new AggregatedScore("""
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
                """, LOG);

        score.gradeMetrics((tool, log) -> createNodes(tool));

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(2).satisfiesExactly(
                first -> assertThat(first).asString().contains("Cyclomatic Complexity: 10", "complexity.png"),
                second -> assertThat(second).asString().contains("Cognitive Complexity: 100", ":brain:"));

        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|10");
    }

    @Test
    void shouldShowScoreWithThreeResults() {
        var score = new AggregatedScore("""
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
                """, LOG);

        score.gradeMetrics((tool, log) -> createNodes(tool));

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
        var root = new ModuleNode(tool.getMetric());
        root.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        root.addValue(new Value(Metric.COGNITIVE_COMPLEXITY, 100));
        root.addValue(new Value(Metric.LOC, 1000));
        return root;
    }

    @Test
    void shouldHandleMissingValue() {
        var score = new AggregatedScore("""
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
                """, LOG);

        var root = new ModuleNode("Root");
        score.gradeMetrics((tool, log) -> root);

        var metricMarkdown = new MetricMarkdown();

        assertThat(metricMarkdown.createSummary(score)).hasSize(1).first().asString().contains(
                "Cyclomatic Complexity: <n/a>");
        assertThat(metricMarkdown.createDetails(score))
                .contains("Toplevel Metrics")
                .contains("|Cyclomatic Complexity|<n/a>");
    }
}
