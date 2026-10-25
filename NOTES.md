# Engineering notes

Running log. Chronological, append-only, written *as things happen* — never
reconstructed afterwards. Bugs, surprises, dead ends, things that took longer
than they should have. This is where the interview stories come from.

Format: a dated heading, then whatever happened. Terse is fine. Honest is mandatory.

---

## 2026-09-20 — Day 0: repo skeleton

- Created the repository and the writing artefacts (README, design doc,
  benchmarks, failure modes, roadmap, this file). No code yet, deliberately —
  each piece gets explained before it's written.
- Toolchain check on the dev machine:
  - JDK **11** (Temurin) is what's on PATH — too old, virtual threads (week 3)
    need 21+. But a full **JDK 24.0.2** is already installed at
    `C:/Program Files/Java/jdk-24`; it's just not on PATH and `JAVA_HOME` is
    unset. No install needed — point `JAVA_HOME` and PATH at jdk-24 before
    week 1. The build will target `--release 21` (the LTS the plan names) so
    the same code compiles on CI with a plain JDK 21.
  - Maven: **not installed**. Plan: install once, generate the wrapper
    (`mvn wrapper:wrapper`), commit it, and use `./mvnw` from then on so the
    laptop and CI run the identical Maven version (ADR-0002).
  - Git 2.49, Docker 29.5, Docker Compose v5.1 — fine.
- Added `.gitattributes` forcing LF line endings. Reason: editing on Windows,
  running on Linux (Docker / CI / Oracle). A script committed with CRLF fails
  there with "bad interpreter", which is a miserable thing to debug on day one.
- Open item carried from the plan: provision the Oracle Cloud A1 instance in
  **week 1**, not week 6. Known for out-of-capacity errors in Indian regions and
  for reclaiming idle instances — find out early whether a fallback is needed.

## 2026-10-25 — The framework: Maven project, entry point, CI

- Hand-wrote `pom.xml` rather than generating it from an archetype, so every
  block has a reason written next to it. Coordinates `io.github.sizzywizzy:switchyard`,
  root package `io.github.sizzywizzy.switchyard`.
- `maven.compiler.release=21` on a laptop that runs JDK 24. CI deliberately uses
  JDK 21 — that run is the proof the "compiled for 21" claim holds. `release`
  also makes javac reject any API newer than 21, which `source`/`target` don't.
- Pinned versions after checking Maven Central: compiler 3.16.0, surefire 3.6.0,
  jar 3.5.1, JUnit 5.14.4, Maven 3.9.16 for the wrapper. Skipped the newer
  lines on purpose: Maven 4 is still RC, the compiler/jar plugins' 4.x are
  betas, and JUnit 6 support in Surefire 3.6 is something I haven't verified.
  Boring and verified beats new and assumed.
- First test is `ToolchainTest` — asserts Java ≥ 21. Looks trivial, but it
  (a) enforces the one environment rule the project has and (b) proves JUnit
  and Surefire are actually wired: a misconfigured Surefire runs zero tests
  and still reports BUILD SUCCESS.
- CI has `permissions: contents: read` — the default token can push, open
  issues, etc.; a build job needs none of that.
- Still to do before the build can run: the Maven wrapper (`mvnw`) is not yet
  in the repo and JDK 11 is still what's on PATH. Expect a Windows-specific
  snag when the wrapper lands: Git on Windows doesn't record the executable
  bit, so `./mvnw` will fail in CI with "Permission denied" unless
  `git update-index --chmod=+x mvnw` is run once.

## 2026-10-25 — First build

- Wrapper in: downloaded the official `maven-wrapper-distribution-3.3.4-only-script.zip`
  (8 KB) from repo1.maven.org, verified its sha1 against Apache's published one,
  kept just `mvnw` + `mvnw.cmd`. The zip has no properties file — wrote
  `.mvn/wrapper/maven-wrapper.properties` by hand, pinned to Maven 3.9.16.
- First `./mvnw verify`: 21 s total including Maven downloading itself to
  `~/.m2/wrapper/dists/apache-maven-3.9.16/`. Surefire auto-detected the JUnit
  Platform provider, ran `ToolchainTest` (1/1), jar built. Nothing to fix.
- Demonstrated why PATH matters: `java -jar` with the JDK 24 binary prints the
  line; the same jar with the Java 11 on PATH dies with `LinkageError` loading
  `Main` — class-file major version 65 (Java 21) is unreadable to an 11 JVM.
  That is the first error anyone with the wrong JDK sees; worth recognising.
- The predicted Windows gotcha, handled: Git on Windows records every file as
  100644, so `./mvnw` would hit "Permission denied" in CI. Fixed in the index
  with `git update-index --chmod=+x mvnw` (now 100755) — the working tree is
  untouched, only what gets committed changes. `.gitattributes` from day 0 did
  its job: `mvnw` stored LF, `mvnw.cmd` CRLF.
- Didn't pin `distributionSha256Sum`: the wrapper deletes the zip after
  extracting, and computing the hash would mean a second 9 MB download just for
  that. HTTPS from Apache's own repo is adequate for a solo project. Revisit if
  anyone else ever builds this.
