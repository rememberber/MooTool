package com.luoboduner.moo.tool.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentVariableServiceTest {

    @Test
    void parsesShellAndEnvironmentFileAssignments() {
        List<EnvironmentVariableService.Entry> entries = EnvironmentVariableService.parseEnvironmentContent("""
                # comment
                export JAVA_HOME='/opt/jdk 21'
                PATH="/usr/local/bin:$PATH"
                EMPTY=
                """);

        assertEquals(List.of(
                new EnvironmentVariableService.Entry("EMPTY", ""),
                new EnvironmentVariableService.Entry("JAVA_HOME", "/opt/jdk 21"),
                new EnvironmentVariableService.Entry("PATH", "/usr/local/bin:$PATH")
        ), entries);
    }

    @Test
    void updatesOnlyTheRequestedAssignmentAndPreservesComments() {
        String updated = EnvironmentVariableService.updateEnvironmentContent(
                "# managed elsewhere\nexport PATH='/old'\nOTHER=\"value\"\n",
                "PATH",
                "/new path",
                true
        );

        assertEquals("# managed elsewhere\nexport PATH='/new path'\nOTHER=\"value\"\n", updated);
        assertEquals("# managed elsewhere\nOTHER=\"value\"\n",
                EnvironmentVariableService.updateEnvironmentContent(updated, "PATH", null, true));
        assertEquals("export QUOTED='it'\\''s safe'\n",
                EnvironmentVariableService.updateEnvironmentContent("", "QUOTED", "it's safe", true));
        assertEquals("it's safe", EnvironmentVariableService.parseEnvironmentContent(
                "export QUOTED='it'\\''s safe'\n").getFirst().value());
    }

    @Test
    void rejectsUnsafeNamesAndMultilineValues() {
        assertThrows(IllegalArgumentException.class,
                () -> EnvironmentVariableService.updateEnvironmentContent("", "BAD-NAME", "value", true));
        assertThrows(IllegalArgumentException.class,
                () -> EnvironmentVariableService.updateEnvironmentContent("", "SAFE_NAME", "one\ntwo", true));
        assertThrows(IllegalArgumentException.class,
                () -> EnvironmentVariableService.set(EnvironmentVariableService.Scope.CURRENT, "SAFE_NAME", "value"));
    }
}
