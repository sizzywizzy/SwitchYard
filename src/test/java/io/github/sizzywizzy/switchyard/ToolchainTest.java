package io.github.sizzywizzy.switchyard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the one environment rule the project has: never build or run on a
 * JDK older than 21. Virtual threads (week 3) do not exist before it.
 *
 * Also the smoke test that proves JUnit 5 and Surefire are wired together.
 * If this test doesn't appear in the build output, the test plumbing is
 * broken, whatever else passes.
 */
class ToolchainTest {

    @Test
    void runsOnJava21OrNewer() {
        int feature = Runtime.version().feature();
        assertTrue(feature >= 21,
                "Expected Java 21+, but the tests are running on Java " + feature
                        + ". Is JAVA_HOME pointing at the wrong JDK?");
    }
}
