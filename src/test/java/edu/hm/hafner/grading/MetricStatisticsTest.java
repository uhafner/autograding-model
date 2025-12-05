package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Value;

import java.util.Locale;
import java.util.NoSuchElementException;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class MetricStatisticsTest {
    @Test
    void shouldAggregateStatistics() {
        MetricStatistics statistics = new MetricStatistics();

        var authors = Metric.AUTHORS.toTagName();
        assertThat(statistics.hasValue(authors)).isFalse();

        statistics.add(new Value(Metric.AUTHORS, 4));

        assertThat(statistics.asMap(Scope.PROJECT)).containsEntry(authors, 4.0);
        assertThat(statistics.asDouble(authors)).isEqualTo(4.0);
        assertThat(statistics.asText(authors, Locale.ENGLISH)).isEqualTo("4");
        assertThat(statistics.hasValue(authors)).isTrue();

        var mutations = Metric.MUTATION.toTagName();
        assertThat(statistics.hasValue(mutations)).isFalse();

        var mutationCoverage = Coverage.valueOf("MUTATION: 2273/2836");
        statistics.add(mutationCoverage);

        var percentage = 2_273 / 2_836.0 * 100;
        assertThat(statistics.asMap(Scope.PROJECT)).containsEntry(mutations, percentage);
        assertThat(statistics.asDouble(mutations)).isEqualTo(percentage);
        assertThat(statistics.asText(mutations, Locale.ENGLISH)).isEqualTo("80.15%");
        assertThat(statistics.hasValue(mutations)).isTrue();
    }

    @Test
    void shouldThrowExceptionsWhenIdIsInvalid() {
        MetricStatistics statistics = new MetricStatistics();

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> statistics.asDouble("invalid-id"));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> statistics.asText("invalid-id", Locale.ENGLISH));

        statistics.add(new Value(Metric.AUTHORS, 4));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> statistics.add(new Value(Metric.AUTHORS, 4)));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> statistics.add(new Value(Metric.AUTHORS, 4), Metric.AUTHORS.toTagName()));    }
}
