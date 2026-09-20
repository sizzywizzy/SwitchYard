# Design

Architecture overview first, then the decision records. Decisions are numbered,
dated, and never deleted — a reversed decision gets a *new* record that
supersedes the old one, so the reasoning trail stays intact.

## Architecture overview

A request enters at the top and leaves at the bottom. Each stage is a filter that
can be switched on or off in config — the same shape Envoy and nginx use.

| # | Stage | Job | Without it |
|---|---|---|---|
| 01 | Listener | Accept the TCP connection, parse bytes into an HTTP request | Nothing works. This is the front door |
| 02 | Router | Read path + `Host`, choose a pool | Layer 4 only — far less interesting to discuss |
| 03 | Rate limit | Token bucket per key / IP / route. `429` with an accurate `Retry-After` | One script takes down every backend at once |
| 04 | Cache | Already have this answer in memory? | Identical work done thousands of times for nothing |
| 05 | Balancer | Given healthy backends and what's known about load and recent latency, pick exactly one | There is no project |
| 06 | Circuit breaker | Closed / open / half-open — is this backend allowed traffic right now? | Keep feeding a server already known to be broken |
| 07 | Proxy | Forward over a pooled keep-alive connection. Handle the timeout. Retry only within budget | Fresh TCP connection per request; latency doubles |
| 08 | Response path | Record duration, update the backend's health score, maybe cache, stream back | Stage 05 chooses blind |

Side modules: `health-checker` (active probes on a schedule + passive tracking of
real failures), `config-manager` (watches the YAML, swaps pool definitions live
without dropping connections), `metrics` (`/metrics` for Prometheus), `admin-api`
(inspect pool state, drain a backend before deploying to it, force a circuit open).

The six-week core is stages 01, 02, 05, 07, 08 plus the health checker and
metrics. Stages 03, 04 and 06 are extensions (E4, E5 / E1, E2).

## Decision records

### How to write one

Each record answers four questions, briefly:

1. **Context** — what situation forced a choice?
2. **Decision** — what was chosen?
3. **Alternatives rejected** — and *why*, specifically.
4. **Consequences** — what this makes easier, what it makes harder, and what
   would change at 100× scale.

Write it when the decision is made, not weeks later. "I chose X because I didn't
know Y existed yet" is honest and useful. A reconstructed record that pretends
otherwise is neither.

### Index

| # | Date | Decision | Status |
|---|---|---|---|
| 0001 | 2026-09-20 | Java 21 with virtual threads for the core; Netty deferred to an extension | accepted |
| 0002 | 2026-09-20 | Maven, via the committed wrapper | accepted |
| 0003 | 2026-09-20 | Docker Compose, not Kubernetes | accepted |
| 0004 | — | Balancing algorithm interface and hot reload | pending (week 5) |
| 0005 | — | Health checking: active + passive + slow-start | pending (week 4) |
| 0006 | — | Backend list concurrency: `CopyOnWriteArrayList` vs `AtomicReference` swap | pending (week 4) |
| 0007 | — | Retry policy: idempotency and the retry budget | pending (E2) |
| 0008 | — | Rate limiter: fail open vs fail closed when Redis is unreachable | pending (E4) |

---

### ADR-0001 — Java 21 with virtual threads for the core; Netty deferred

**Context.** The balancer must hold thousands of concurrent connections. One
platform thread per connection costs roughly a megabyte of stack each, and past
a few hundred the OS spends its time context-switching instead of working. The
two ways out are an event loop (Netty) or virtual threads (JDK 21+).

**Decision.** Build the core with plain blocking I/O, and switch the executor to
virtual threads in week 3. The code keeps its simple, readable, blocking shape.
Netty becomes extension E3, where the deliverable is the *three-way comparison*
(platform threads vs virtual threads vs event loop) rather than Netty itself.

**Alternatives rejected.**
- *Netty from week 3, as the original plan had it.* For someone learning `ByteBuf`
  reference counting, the pipeline model and backpressure cold, that is realistically
  two to three weeks — and every later week depended on it. One slip stranded the
  whole project.
- *Stay on platform threads.* Caps the project at a few hundred connections and
  makes every later benchmark meaningless.

**Consequences.**
- Requires JDK 21+. The dev machine has JDK 24 installed (not on PATH as of this
  writing); the build targets `--release 21` so CI can run the LTS.
- Must understand *pinning* — a virtual thread blocking its carrier inside a
  `synchronized` block or a native call. This is the gotcha to watch for, and a
  genuinely good interview detail.
- At 100× scale an event loop with zero-copy buffers wins on CPU per request.
  That is exactly what E3 measures.

### ADR-0002 — Maven, via the committed wrapper

**Context.** Need a build, a test runner, and a CI step that behaves identically
on the laptop and on GitHub Actions.

**Decision.** Maven. Commit the Maven wrapper (`mvnw`, `mvnw.cmd`,
`.mvn/wrapper/`) so every environment runs the same Maven version without a
global install.

**Alternatives rejected.** Gradle — perfectly good, but Maven's rigid, boring
layout is easier to learn from, and the plan's CI step is literally `mvn test`.
Not worth a second learning curve alongside sockets and concurrency.

**Consequences.** Standard `src/main/java` / `src/test/java` layout. One
`pom.xml` that gets explained line by line the first time it's written.

### ADR-0003 — Docker Compose, not Kubernetes

**Context.** The stack is a balancer, three backends, Prometheus, Grafana, and
eventually Redis. It needs to come up with one command on a laptop and on the
Oracle box.

**Decision.** Docker Compose.

**Alternatives rejected.** Kubernetes — the single most common way a student
project of this kind dies. Nothing here needs scheduling, service discovery or
multi-node anything. It is one host.

**Consequences.** "Why not Kubernetes?" becomes a question with a good answer
(one host, no need). One `docker-compose.yml` is the entire deployment.
