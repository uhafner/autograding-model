package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DatabindException;

import static org.assertj.core.api.Assertions.*;

abstract class AbstractConfigurationTest {
    @Test
    void shouldReportInvalidConfigurations() {
        assertThatExceptionOfType(JacksonException.class)
                .isThrownBy(() -> fromJson(getInvalidJson()))
                .withMessageContaining("Unexpected character");
    }

    @Test
    void shouldReportEmptyConfigurations() {
        assertThatExceptionOfType(DatabindException.class)
                .isThrownBy(() -> fromJson("""
                {
                  "tests": false,
                  "analysis": false,
                  "coverage": false
                }
                """))
                .withMessageContaining("Cannot construct instance");
    }

    protected abstract List<? extends Configuration> fromJson(String json);

    protected abstract String getInvalidJson();
}
