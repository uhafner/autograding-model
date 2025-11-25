package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.grading.assertions.Assertions.*;

class ToolConfigurationTest {
    @Test
    void shouldCreateTool() {
        var toolConfiguration = new ToolConfiguration("spotbugs", "SpotBugs", "target/spotbugsXml.xml", "", "", "", "");

        assertThat(toolConfiguration).hasId("spotbugs").hasName("SpotBugs").hasPattern("target/spotbugsXml.xml");
    }

    @Test
    void shouldCreateToolWithDefaultScope() {
        var toolConfiguration = new ToolConfiguration("", "", "", "", "", "", "");

        assertThat(toolConfiguration).hasScope(Scope.PROJECT);
    }

    @Test
    void shouldCreateToolWithAppropriateScope() {
        var projectToolConfiguration = new ToolConfiguration("", "", "", "", "", "project", "");
        assertThat(projectToolConfiguration).hasScope(Scope.PROJECT);

        var filesToolConfiguration = new ToolConfiguration("", "", "", "", "", "modified_files", "");
        assertThat(filesToolConfiguration).hasScope(Scope.MODIFIED_FILES);

        var linesToolConfiguration = new ToolConfiguration("", "", "", "", "", "modified_lines", "");
        assertThat(linesToolConfiguration).hasScope(Scope.MODIFIED_LINES);
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(ToolConfiguration.class).verify();
    }
}
