package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.grading.MetricScore.MetricScoreBuilder;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.grading.assertions.Assertions.*;

/**
 * Tests the class {@link MetricScore}.
 *
 * @author Ullrich Hafner
 */
class MetricScoreTest {
    private static final String NAME = "Metric Name";

    @Test
    void shouldCreateInstanceAndGetProperties() {
        var configuration = createConfiguration();
        var report = createReport(Metric.CYCLOMATIC_COMPLEXITY, 10, "root");
        var score = new MetricScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration)
                .create(report, Metric.CYCLOMATIC_COMPLEXITY);

        assertThat(score)
                .hasName(NAME)
                .hasConfiguration(configuration)
                .hasMetric(Metric.CYCLOMATIC_COMPLEXITY)
                .hasReport(report)
                .hasMaxScore(0)
                .hasImpact(0);

        assertThat(score.getMetricValue()).isEqualTo(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        assertThat(score.getMetricValueAsString()).isEqualTo("10");
        assertThat(score.getMetricTagName()).isEqualTo("cyclomatic-complexity");
    }

    @Test
    void shouldHandleMissingMetricValue() {
        var configuration = createConfiguration();
        var report = new ModuleNode("empty");
        var score = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .create(report, Metric.CYCLOMATIC_COMPLEXITY);

        assertThat(score.getMetricValue()).isEqualTo(Value.nullObject(Metric.CYCLOMATIC_COMPLEXITY));
        assertThat(score.getMetricValueAsString()).isEqualTo("<n/a>");
    }

    @Test
    void shouldAggregateDifferentMetrics() {
        var configuration = createConfiguration();

        var red = createReport(Metric.CYCLOMATIC_COMPLEXITY, 10, "red");
        var redScore = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .create(red, Metric.CYCLOMATIC_COMPLEXITY);

        var blue = createReport(Metric.LOC, 100, "report2");
        var blueScore = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .create(blue, Metric.LOC);

        var aggregated = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .aggregate(List.of(redScore, blueScore));

        assertThat(aggregated.getMetric()).isEqualTo(Metric.CONTAINER);
        assertThat(aggregated.getMetricValueAsString()).isEqualTo("<n/a>");
    }

    @Test
    void shouldAggregateSameMetrics() {
        var configuration = createConfiguration();

        var report1 = createReport(Metric.CYCLOMATIC_COMPLEXITY, 10, "report1");
        var score1 = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .create(report1, Metric.CYCLOMATIC_COMPLEXITY);

        var report2 = createReport(Metric.CYCLOMATIC_COMPLEXITY, 20, "report2");
        var score2 = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .create(report2, Metric.CYCLOMATIC_COMPLEXITY);

        var aggregated = new MetricScoreBuilder()
                .setConfiguration(configuration)
                .aggregate(List.of(score1, score2));

        assertThat(aggregated.getMetric()).isEqualTo(Metric.CYCLOMATIC_COMPLEXITY);
        assertThat(aggregated.getMetricValue()).isEqualTo(new Value(Metric.CYCLOMATIC_COMPLEXITY, 30));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(MetricScore.class)
                .withRedefinedSuperclass()
                .withIgnoredFields("report")
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    private MetricConfiguration createConfiguration() {
        return MetricConfiguration.from("""
                {
                  "metrics": [{
                    "tools": [{
                      "id": "metrics",
                      "pattern": "target/*.xml"
                    }]
                  }]
                }
                """).get(0);
    }

    private Node createReport(final Metric metric, final int value, final String name) {
        var root = new ModuleNode(name);
        root.addValue(new Value(metric, value));
        return root;
    }
}
