package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

abstract class AbstractConfigurationTest {
    @Test
    void shouldReportInvalidConfigurations() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> fromJson(getInvalidJson()))
                .withMessageContaining("Can't convert JSON")
                .withCauseInstanceOf(JsonParseException.class);
    }

    @Test
    void shouldReportEmptyConfigurations() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> fromJson("""
                {
                  "tests": false,
                  "analysis": false,
                  "coverage": false
                }
                """))
                .withMessageContaining("Can't convert JSON")
                .withCauseInstanceOf(JsonMappingException.class);
    }

    protected abstract List<? extends Configuration> fromJson(String json);

    protected abstract String getInvalidJson();
}
