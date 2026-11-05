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
- CI green on the first run — JDK 21 on Ubuntu, 19 s wall time including
  Maven and dependency downloads (no cache yet; `cache: maven` warms it from
  here). https://github.com/sizzywizzy/SwitchYard/actions/runs/40191605299

## 2026-11-05 — Week 1: listener → threads → proxy → bodies

Four steps, each committed separately (`902ff92` listener, `af16ace` thread per
connection, `26dac2e` forwarding + `Backend`; step 4 — bodies — committed after
this entry). What was actually seen, in order:

- **The raw request, by hand.** `curl -v localhost:8080` and the server printed the
  same four lines curl showed with `>`. HTTP stopped being abstract.
- **One stream object, bytes only — the bug of the week.** First proxy version read
  the head with a `BufferedReader`. `curl -d "abc"` returned `200` and the backend
  never saw `abc`. No error anywhere; the reader had slurped the body into its
  private buffer and the raw stream had nothing left. Worse than a crash because
  it looked like success. Fix: `Http.readHead` reads one byte at a time off a
  `BufferedInputStream` and stops exactly at `\r\n\r\n`; `readNBytes(Content-Length)`
  takes the body from the *same* stream object. `HttpTest` pins it: after
  `readHead`, the body must still be readable. Verified today: backend log shows
  `POST / HTTP/1.1  body: abc`.
- **End-of-stream is only a signal when someone promises to close.** Toward the
  backend we add `Connection: close`, so `transferTo` on the reply legitimately
  runs "until EOF". Toward the client we never rely on EOF — the client is waiting
  for us. Same question, "where does the message end?", answered three ways in one
  file: blank line (head), `Content-Length` (body), close (backend reply).
- **`?delay=1500` through the proxy took 1615 ms.** The delay lives in the backend;
  Switchyard just waited. That ~100 ms of overhead is JVM warm-up plus curl process
  start — not a number to quote anywhere; see BENCHMARKS.md for how to measure it.
- **Backend killed → `502 Bad Gateway`, Switchyard survived, restarted backend
  picked up with no proxy restart.** First real row in FAILURE_MODES.md. Note what
  was *not* there: no detection, no log line (stderr stayed empty — the
  `ConnectException` is caught and turned into the 502). Week 4's job.
- **Lost an hour to a stale process.** Rebuilt, ran the new jar, got the old
  behaviour. The new instance had died on `BindException: Address already in use`
  because an earlier Switchyard was still holding 8080 from another terminal, and
  every curl was being answered by it. Lesson, now a habit:
  `netstat -ano | findstr :8080` before believing any result.
- **Near miss in the editor.** `Backend.java` was being hand-written in VS Code
  (unsaved) while a complete version was written to disk. VS Code kept the dirty
  buffer; one Ctrl+S would have overwritten the working file with a half-finished
  one. Editor buffer ≠ disk.
- **Known limits, left on purpose:** `Transfer-Encoding: chunked` bodies are treated
  as empty; `Content-Length` is trusted (a huge value would make `readNBytes`
  allocate it); the forwarded `Host` header still says `localhost:8080`; a client's
  own `Connection` header would be forwarded alongside ours. Each gets a row or an
  ADR when it matters.
