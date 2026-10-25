package io.github.sizzywizzy.switchyard;

/**
 * Entry point.
 *
 * Deliberately does nothing yet. It exists so the build has something to
 * compile, test, package and run end to end before any real logic arrives.
 * The listener (week 1) will replace the body of {@code main}.
 */
public final class Main {

    private Main() {
        // Not instantiable: this class only hosts main().
    }

    public static void main(String[] args) {
        // Runtime.version() is the JVM actually running us (24 on the laptop,
        // 21 in CI). The bytecode was compiled for 21 regardless — see pom.xml.
        System.out.println("Switchyard: running on Java " + Runtime.version().feature()
                + ", compiled for Java 21. Nothing to proxy yet.");
    }
}
