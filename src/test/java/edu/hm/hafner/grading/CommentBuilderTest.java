package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;
import edu.hm.hafner.util.FilteredLog;

import static edu.hm.hafner.grading.AnalysisMarkdownTest.*;
import static org.assertj.core.api.Assertions.*;
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
    void shouldCreateCoverageComments() {
        var aggregation = createCoverageAggregation();

        var builder = spy(CommentBuilder.class);

        builder.createAnnotations(aggregation);

        verify(builder, times(7))
                .createComment(any(), anyString(), anyInt(), anyInt(), anyString(), anyString(),
                        anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void shouldLimitCoverageComments() {
        var aggregation = createCoverageAggregation();

        var builder = spy(CommentBuilder.class);
        when(builder.getMaxCoverageComments()).thenReturn(5);

        builder.createAnnotations(aggregation);

        verify(builder, times(5))
                .createComment(any(), anyString(), anyInt(), anyInt(), anyString(), anyString(),
                        anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void shouldCreateWarningComments() {
        var aggregation = createWarningsAggregation();

        var builder = spy(CommentBuilder.class);

        builder.createAnnotations(aggregation);

        verify(builder, times(10))
                .createComment(any(), anyString(), anyInt(), anyInt(), anyString(), anyString(),
                        anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void shouldLimitWarningComments() {
        var aggregation = createWarningsAggregation();

        var builder = spy(CommentBuilder.class);
        when(builder.getMaxWarningComments()).thenReturn(5);

        builder.createAnnotations(aggregation);

        verify(builder, times(5))
                .createComment(any(), anyString(), anyInt(), anyInt(), anyString(), anyString(),
                        anyInt(), anyInt(), anyString(), anyString());
    }

    @ParameterizedTest(name = "Should show description: {0}")
    @ValueSource(booleans = {true, false})
    void shouldShowOrHideDescription(final boolean hideDescription) {
        var aggregation = createWarningsAggregation();

        var builder = spy(CommentBuilder.class);
        when(builder.getMaxWarningComments()).thenReturn(1);
        when(builder.isWarningDescriptionHidden()).thenReturn(hideDescription);

        builder.createAnnotations(aggregation);

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);

        verify(builder, times(1))
                .createComment(any(), anyString(), anyInt(), anyInt(), anyString(), anyString(),
                        anyInt(), anyInt(), anyString(), description.capture());

        if (hideDescription) {
            assertThat(description.getValue()).isEmpty();
        }
        else {
            assertThat(description.getValue()).contains("<p>Since Checkstyle 3.1</p>");
        }
    }

    private AggregatedScore createWarningsAggregation() {
        var aggregation = new AggregatedScore("""
                {
                  "analysis": [{
                    "tools": [
                      {
                        "id": "checkstyle",
                        "pattern": "target/checkstyle.xml"
                      }
                    ],
                    "name": "CheckStyle",
                    "errorImpact": -1,
                    "highImpact": -2,
                    "normalImpact": -3,
                    "lowImpact": -4,
                    "maxScore": 100
                  }]
                }
                """, new FilteredLog("Test"));
        aggregation.gradeAnalysis((tool, log) -> createSampleReport());
        return aggregation;
    }

    private AggregatedScore createCoverageAggregation() {
        var aggregation = new AggregatedScore(COVERAGE_CONFIGURATION, new FilteredLog("Test"));
        aggregation.gradeCoverage((tool, log)
                -> AggregatedScoreTest.readCoverageReport("mutations-dashboard.xml", tool, CoverageParserType.PIT));
        return aggregation;
    }
}
