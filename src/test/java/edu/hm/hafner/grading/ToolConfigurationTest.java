package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class ToolConfigurationTest {
    @Test
    void shouldCreateTool() {
        var toolConfiguration = new ToolConfiguration("spotbugs", "SpotBugs", "target/spotbugsXml.xml");

        assertThat(toolConfiguration).hasId("spotbugs").hasName("SpotBugs").hasPattern("target/spotbugsXml.xml");
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(ToolConfiguration.class).verify();
    }
}
