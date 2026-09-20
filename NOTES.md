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
