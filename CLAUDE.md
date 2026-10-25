# Switchyard

Layer 7 HTTP load balancer, written from scratch in Java. Personal project built
alongside placement prep. Six-week shippable core, then ranked extensions.
Roadmap: `docs/roadmap.md`. Decisions and reasoning: `docs/design.md`.

## How we work on this — read before doing anything

- **Explain, then write.** Every piece of code is walked through *before* it lands:
  what it does, why it exists, what would break without it. No unexplained code and
  no large drops of finished implementation. Understanding is the goal, not speed.
- **One step at a time.** Build the smallest thing that works, run it, watch it
  work, then the next thing. The ugly-but-clear version comes first on purpose
  (week 1 is one-thread-per-connection, blocking).
- **DSA hours are fixed and come first.** This project fits into what remains.
  Never propose a pace or scope that assumes otherwise.
- **No number without a reproduction.** Nothing goes in `BENCHMARKS.md`, the README
  or a resume until it has been measured on a clean setup with the topology
  written down. An unlabelled latency figure is a liability.
- **Cut features, not writing.** If time runs short, the design doc, failure-modes
  file and notes outrank the next feature.
- **`NOTES.md` is written as things happen**, never reconstructed. Every bug
  fought, every surprise. That's where interview stories come from.

## Locked decisions (reasoning in docs/design.md)

| Decision | Choice |
|---|---|
| Language / runtime | Java 21 — virtual threads are required from week 3 |
| Build | Maven, via the committed wrapper (`mvnw`) |
| Core concurrency model | Blocking I/O on virtual threads. Netty is extension E3, not a prerequisite |
| Deployment | Docker Compose on an Oracle Cloud always-free ARM (A1) instance |
| Observability | Micrometer → Prometheus → Grafana. Grafana *is* the UI; no custom dashboard |
| Out of scope | Kubernetes, Spring in the balancer core, Kafka, Raft/consensus, HTTP/2 & gRPC (until HTTP/1.1 keep-alive is solid), TLS internals (Caddy terminates) |

## Repo map

| Path | What it is |
|---|---|
| `README.md` | Front door: what it is, architecture, quickstart, honest limitations |
| `docs/roadmap.md` | Week-by-week plan with observable milestones. Ship line after week 6 |
| `docs/design.md` | Architecture overview + decision records (ADRs) |
| `BENCHMARKS.md` | Methodology and topology first, then numbers, then the breaking point |
| `FAILURE_MODES.md` | What breaks, how it's detected, how the system behaves while broken |
| `NOTES.md` | Running engineering log, chronological, append-only |
| `pom.xml` | The build. Every block is commented in place; targets `--release 21` |
| `src/main/java/io/github/sizzywizzy/switchyard/` | Java sources. Root package `io.github.sizzywizzy.switchyard` |
| `src/test/java/...` | JUnit 5 tests, same package layout |
| `.github/workflows/ci.yml` | CI: JDK 21 on Ubuntu, `./mvnw verify` on every push |
| `config/` | *(not yet)* `switchyard.yaml` — pools, algorithm, timeouts |
| `backends/` | *(not yet)* the tiny test backends that return their own name |
| `grafana/` | *(not yet)* committed dashboard JSON |

## Environment (as of 2026-09-20)

- Windows 11, Git Bash available. Git 2.49, Docker 29.5, Compose v5.1 — fine.
- JDK on PATH is **11**, but a full **JDK 24.0.2** exists at
  `C:/Program Files/Java/jdk-24` (not on PATH, `JAVA_HOME` unset). Use that.
  Maven is **not installed**. Both must be sorted before the first line of
  Java (see NOTES.md). Build targets `--release 21`; never write against 11.
