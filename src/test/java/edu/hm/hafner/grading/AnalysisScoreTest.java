package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.AnalysisScore.AnalysisScoreBuilder;

import java.util.List;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class AnalysisScoreTest {
    private static final String NAME = "Results";
    private static final ParserRegistry PARSER_REGISTRY = new ParserRegistry();

    @Test
    void shouldCalculateImpactAndScoreWithNegativeValues() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "errorImpact": -4,
                    "highImpact": -3,
                    "normalImpact": -2,
                    "lowImpact": -1,
                    "maxScore": 25
                  }
                }
                """);

        var analysisScore = createScore(configuration);
        assertThat(analysisScore)
                .hasName(NAME).hasConfiguration(configuration)
                .hasErrorSize(2).hasHighSeveritySize(2).hasNormalSeveritySize(2).hasLowSeveritySize(2)
                .hasMaxScore(25)
                .hasImpact(2 * -4 - 2 * 3 - 2 * 2 - 2)
                .hasValue(5);

        for (Severity severity : Severity.getPredefinedValues()) {
            assertThat(analysisScore.getReport().getSizeOf(severity)).isEqualTo(2);
        }
        assertThat(analysisScore.toString()).startsWith("{")
                .endsWith("}")
                .containsIgnoringWhitespaces("\"impact\":-20");
    }

    @Test
    void shouldCalculateImpactAndScoreWithPositiveValues() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "errorImpact": 4,
                    "highImpact": 3,
                    "normalImpact": 2,
                    "lowImpact": 1,
                    "maxScore": 25
                  }
                }
                """);

        var analysisScore = createScore(configuration);
        assertThat(analysisScore)
                .hasName(NAME).hasConfiguration(configuration)
                .hasErrorSize(2).hasHighSeveritySize(2).hasNormalSeveritySize(2).hasLowSeveritySize(2)
                .hasMaxScore(25)
                .hasTotalSize(2 + 2 + 2 + 2)
                .hasImpact(2 * 4 + 2 * 3 + 2 * 2 + 2)
                .hasValue(20);
    }

    private AnalysisScore createScore(final AnalysisConfiguration configuration) {
        return new AnalysisScoreBuilder()
                .setName(NAME)
                .setConfiguration(configuration)
                .create(createReportWith(Severity.ERROR, Severity.ERROR,
                        Severity.WARNING_HIGH, Severity.WARNING_HIGH,
                        Severity.WARNING_NORMAL, Severity.WARNING_NORMAL,
                        Severity.WARNING_LOW, Severity.WARNING_LOW));
    }

    @Test
    void shouldComputePositiveImpactBySizeZero() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "name": "Checkstyle and SpotBugs",
                    "errorImpact": 100,
                    "highImpact": 100,
                    "normalImpact": 100,
                    "lowImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new AnalysisScoreBuilder()
                .setConfiguration(configuration)
                .create(new Report());
        assertThat(score)
                .hasImpact(0)
                .hasValue(0);
    }

    @Test
    void shouldComputeNegativeImpactBySizeZero() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "name": "Checkstyle and SpotBugs",
                    "errorImpact": -100,
                    "highImpact": -100,
                    "normalImpact": -100,
                    "lowImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new AnalysisScoreBuilder()
                .setConfiguration(configuration)
                .create(new Report());
        assertThat(score)
                .hasImpact(0)
                .hasValue(50);
    }

    @Test
    void shouldHandleOverflowWithPositiveImpact() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "errorImpact": 100,
                    "highImpact": 100,
                    "normalImpact": 100,
                    "lowImpact": 100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new AnalysisScoreBuilder().setConfiguration(configuration).create(
                createReportWith(Severity.ERROR, Severity.WARNING_HIGH, Severity.WARNING_NORMAL, Severity.WARNING_LOW));
        assertThat(score)
                .hasImpact(400)
                .hasValue(50);
    }

    @Test
    void shouldHandleOverflowWithNegativeImpact() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "errorImpact": -100,
                    "highImpact": -100,
                    "normalImpact": -100,
                    "lowImpact": -100,
                    "maxScore": 50
                  }
                }
                """);

        var score = new AnalysisScoreBuilder().setConfiguration(configuration).create(
                createReportWith(Severity.ERROR, Severity.WARNING_HIGH, Severity.WARNING_NORMAL, Severity.WARNING_LOW));
        assertThat(score)
                .hasImpact(-400)
                .hasValue(0);
    }

    @Test
    void shouldCreateSubScores() {
        var configuration = createConfiguration("""
                {
                  "analysis": {
                    "tools": [
                        {
                          "id": "spotbugs",
                          "name": "SpotBugs",
                          "pattern": "target/spotbugsXml.xml"
                        }
                      ],
                    "errorImpact": 3,
                    "highImpact": 1,
                    "normalImpact": 1,
                    "lowImpact": 1,
                    "maxScore": 100
                  }
                }
                """);

        var builder = new AnalysisScoreBuilder();
        builder.setConfiguration(configuration);

        var first = builder.create(
                createReportWith(Severity.ERROR, Severity.WARNING_HIGH, Severity.WARNING_NORMAL, Severity.WARNING_LOW));
        assertThat(first).hasImpact(6).hasValue(6);
        var second = builder.create(
                createReportWith(Severity.WARNING_LOW, Severity.WARNING_NORMAL));
        assertThat(second).hasImpact(2).hasValue(2);

        var aggregation = new AnalysisScoreBuilder()
                .setConfiguration(configuration)
                .setName("Aggregation")
                .aggregate(List.of(first, second));
        assertThat(aggregation)
                .hasImpact(6 + 2)
                .hasValue(6 + 2)
                .hasName("Aggregation")
                .hasOnlySubScores(first, second);

        var overflow = new AnalysisScoreBuilder();
        var score = overflow.setConfiguration(createConfiguration("""
                        {
                          "analysis": {
                            "tools": [
                                {
                                  "id": "spotbugs",
                                  "name": "SpotBugs",
                                  "pattern": "target/spotbugsXml.xml"
                                }
                              ],
                            "errorImpact": 3,
                            "highImpact": 1,
                            "normalImpact": 1,
                            "lowImpact": 1,
                            "maxScore": 7
                          }
                        }
                        """))
                .setName("Aggregation")
                .aggregate(List.of(first, second));
        assertThat(score).hasImpact(6 + 2).hasValue(7).hasName("Aggregation");
    }

    static Report createReportWith(final Severity... severities) {
        return createReportWith("checkstyle", "CheckStyle", severities);
    }

    static Report createReportWith(final String id, final String name, final Severity... severities) {
        var report = new Report();
        try (var builder = new IssueBuilder()) {
            for (int i = 0; i < severities.length; i++) {
                var severity = severities[i];
                var text = severity.toString() + "-" + i;
                report.add(builder.setMessage(text)
                        .setFileName(text)
                        .setType("DesignForExtensionCheck")
                        .setSeverity(severity).build());
            }
        }

        report.setOrigin(id, name, PARSER_REGISTRY.get(id).getType(), name + ".xml");
        return report;
    }

    private AnalysisConfiguration createConfiguration(final String json) {
        return AnalysisConfiguration.from(json).get(0);
    }
}
