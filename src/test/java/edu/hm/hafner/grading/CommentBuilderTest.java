package edu.hm.hafner.grading;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;
import edu.hm.hafner.util.FilteredLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private static final String REVAPI_CONFIGURATION = """
            {
                "analysis": {
                  "name": "API Problems",
                  "id": "api",
                  "icon": "no_entry_sign",
                  "tools": [
                    {
                      "id": "revapi",
                      "sourcePath": "src/main/java",
                      "pattern": "revapi-result.json"
                    }
                  ]
                }
            }
            """;

    @Test
    void shouldCreateRevApiComments() {
        var score = new AggregatedScore(new FilteredLog("Test"));
        score.gradeAnalysis(new ReportSupplier(this::readAnalysisReport),
                AnalysisConfiguration.from(REVAPI_CONFIGURATION), Optional.empty());

        var builder = spy(CommentBuilder.class);

        builder.createAnnotations(score);

        var commentCaptor = ArgumentCaptor.forClass(String.class);
        var markdownCaptor = ArgumentCaptor.forClass(String.class);
        var messageCaptor = ArgumentCaptor.forClass(String.class);
        var titleCaptor = ArgumentCaptor.forClass(String.class);
        var pathCaptor = ArgumentCaptor.forClass(String.class);
        var lineStartCaptor = ArgumentCaptor.forClass(Integer.class);
        var lineEndCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(builder, times(35)).createComment(
                any(), pathCaptor.capture(), lineStartCaptor.capture(), lineEndCaptor.capture(),
                messageCaptor.capture(), titleCaptor.capture(), anyInt(), anyInt(),
                commentCaptor.capture(), markdownCaptor.capture());

        assertThat(titleCaptor.getAllValues()).hasSize(35).first().asString().isEqualTo("Revapi: java.class.externalClassExposedInAPI");
        assertThat(messageCaptor.getAllValues()).hasSize(35).first().asString().isEqualTo("A class from supplementary archives is used in a public capacity in the API.");
        assertThat(pathCaptor.getAllValues()).hasSize(35).first().asString().isEqualTo("edu/hm/hafner/analysis/Issue.java");
        assertThat(lineStartCaptor.getAllValues()).hasSize(35).first().isEqualTo(0);
        assertThat(lineEndCaptor.getAllValues()).hasSize(35).first().isEqualTo(0);
        assertThat(commentCaptor.getAllValues()).hasSize(35).first().asString().isEmpty();
        assertThat(markdownCaptor.getAllValues()).hasSize(35).first().asString().isEqualToIgnoringWhitespace("""
                    <table>
                          <tr>
                              <td>
                                  Class:
                              </td>
                              <td>
                                  edu.hm.hafner.analysis.Issue
                              </td>
                          </tr>
                          <tr>
                              <td>
                                  Code:
                              </td>
                              <td>
                                  java.class.externalClassExposedInAPI
                              </td>
                          </tr>
                          <tr>
                              <td>
                                  Name:
                              </td>
                              <td>
                                  external class in API
                              </td>
                          </tr>
                          <tr>
                              <td>
                                  New Element:
                              </td>
                              <td>
                                  missing-class edu.hm.hafner.analysis.Issue
                              </td>
                          </tr>
                          <tr>
                              <td>
                                  Old Element:
                              </td>
                              <td>
                                  -
                              </td>
                          </tr>
                          <tr>
                              <td>
                                  Justification:
                              </td>
                              <td>
                                  -
                              </td>
                          </tr>
                          <tr>
                              <td>
                                  Classification:
                              </td>
                              <td>
                                  <dl>
                                      <dt>SOURCE</dt> <dd>NON_BREAKING</dd>
                                      <dt>BINARY</dt> <dd>NON_BREAKING</dd>
                                      <dt>SEMANTIC</dt> <dd>POTENTIALLY_BREAKING</dd>
                                  </dl>
                              </td>
                          </tr>
                      </table>
                  """);
    }

    private Report readAnalysisReport(final ToolConfiguration tool) {
        try {
            var registry = new ParserRegistry();
            return registry.get(tool.getId())
                    .createParser()
                    .parse(new FileReaderFactory(createPath(tool.getPattern())));
        }
        catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private Path createPath(final String fileName) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(AggregatedScoreTest.class.getResource(
                fileName), "File not found: " + fileName).toURI());
    }

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
        var configuration = """
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
                """;
        var aggregation = new AggregatedScore(new FilteredLog("Test"));
        aggregation.gradeAnalysis(new ReportSupplier(
                t -> AnalysisMarkdownTest.createSampleReport()),
                AnalysisConfiguration.from(configuration), Optional.empty());
        return aggregation;
    }

    private AggregatedScore createCoverageAggregation() {
        var aggregation = new AggregatedScore(new FilteredLog("Test"));
        aggregation.gradeCoverage(
                new NodeSupplier(t ->
                        AggregatedScoreTest.readCoverageReport("mutations-dashboard.xml", CoverageParserType.PIT, "mutations-dashboard.xml")),
                CoverageConfiguration.from(COVERAGE_CONFIGURATION), Optional.empty());
        return aggregation;
    }
}
