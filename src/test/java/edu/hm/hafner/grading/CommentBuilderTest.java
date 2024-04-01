package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;
import edu.hm.hafner.util.FilteredLog;

import static org.mockito.Mockito.*;

class CommentBuilderTest {
    private static final String COVERAGE_CONFIGURATION = """
            {
              "coverage": [
              {
                  "tools": [
                      {
                        "id": "pit",
                        "name": "Mutation Coverage",
                        "metric": "mutation",
                        "pattern": "**/src/**/mutations-dashboard.xml"
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

    @Test
    void shouldRunComments() {
        var aggregation = new AggregatedScore(COVERAGE_CONFIGURATION, new FilteredLog("Test"));
        aggregation.gradeCoverage((tool, log) -> AggregatedScoreTest.readCoverageReport(
                "mutations-dashboard.xml", tool, CoverageParserType.PIT));

        CommentBuilder builder = spy(CommentBuilder.class);

        builder.createAnnotations(aggregation);

        verify(builder, times(7))
                .createComment(any(), anyString(), anyInt(), anyInt(), anyString(), anyString(),
                        anyInt(), anyInt(), anyString(), anyString());
    }
}
