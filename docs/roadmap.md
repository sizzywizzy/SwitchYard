# Roadmap

Six-week core, then a ship line, then ranked extensions taken one at a time.
Stop at the ship line whenever placement season takes the hours back — a
finished six-week project beats an abandoned twelve-week one every time.

Standing rule: **DSA hours are fixed and come first.** This fits into what's left.

Tick a milestone only when it has been *observed*, not when the code compiles.

## Where we are — 2026-11-05

Week 1 code is complete and every behaviour has been observed: listener, thread per
connection, forwarding with request bodies, `502` on a dead backend, automatic
recovery. Five tests green. Two things stand between this and the week-1 milestone:

1. **Commit step 4** (`Http.java`, `HttpTest.java`, `Main.java`, `Backend.java`) plus
   the docs — the only uncommitted work.
2. **Oracle Cloud** — signup started by you; the always-free ARM tier is often out
   of capacity in Indian regions, so start now and let it churn. Week 2 deploys to it.

Then week 2, step 1: round robin across three backends.

## Week 1 — The dumbest proxy that works

Blocking, one thread per connection, one hardcoded backend, bytes copied across.
Ugly on purpose — it's the baseline everything later gets measured against.

- [x] Maven via the committed wrapper (3.9.16); `pom.xml` written and explained; `./mvnw verify` green locally (2026-10-25)
- [x] GitHub Actions: build + tests on every push, green (first run green 2026-10-25)
- [x] `ServerSocket` accepts a connection; the request is printed as raw text (`902ff92`)
- [x] One thread per connection — the idle-client freeze seen, then gone (`af16ace`)
- [x] Step 3 observed end to end (2026-11-05): `curl.exe localhost:8080` → `hello from backend-1`, `?delay=2000` took 2123 ms, POST body silently dropped (flaw #3 confirmed)
- [x] Backend killed → `502 Bad Gateway`, Switchyard still up; backend restarted, next curl works with no proxy restart (observed 2026-11-05)
- [x] Bytes forwarded to one hardcoded backend and the response copied back (`26dac2e`, observed 2026-11-05)
- [x] Tiny backend that returns its own name and honours `?delay=` (`Backend.java`; `?delay=1500` observed 2026-11-05 — three instances come with round robin)
- [x] Request *bodies* survive the proxy: `curl.exe -d "abc"` → backend logged `body: abc` (step 4, observed 2026-11-05; `HttpTest` guards it)
- [x] `NOTES.md` week-1 entry written from the runs; `FAILURE_MODES.md` first two rows filled (2026-11-05)
- [x] `JAVA_HOME` → jdk-24 set at user scope (2026-11-05). `java` on PATH is still 11; run the jar with the JDK 24 binary, full path
- [ ] Oracle Cloud A1 instance provisioned — or a fallback chosen if out of capacity

**Milestone:** `curl localhost:8080` returns "hello from backend-1", CI is green,
the Oracle box exists.

## Week 2 — Many backends, config, tests

- [ ] Round robin across three backends
- [ ] Backend list moved into `config/switchyard.yaml`
- [ ] Structured JSON logging — every request logs the chosen backend and duration
- [ ] **The test suite.** Server bound to port 0, real client, assert on the response
- [ ] First deploy to the Oracle box; deploy on every merge from here on

**Milestone:** nine requests land 3-3-3, the logs prove it, the deployed URL
serves the same thing.

## Week 3 — Concurrency via virtual threads

- [ ] Measure where the week-1 blocking version dies (expect somewhere around 200)
- [ ] Swap the executor to `Executors.newVirtualThreadPerTaskExecutor()`
- [ ] Hunt for pinning (`synchronized` blocks, native calls)
- [ ] Tests still green — that's what they're for

**Milestone:** 5,000+ concurrent connections held open, with the week-1 figure
recorded alongside for the later comparison.

## Week 4 — Health checking

- [ ] Active: background probe of every backend's `/health` every 2 s
- [ ] Passive: eject after 3 consecutive timeouts or 5xx, whatever `/health` says
- [ ] Slow-start: a recovered backend gets 10% and ramps over 30 s
- [ ] Backend list safe to read while it's being replaced (ADR-0006)

**Milestone:** `docker stop backend-2` mid-load-test → zero failed requests,
traffic rebalanced in under 2 s.

## Week 5 — Balancing algorithms

- [ ] One `Balancer` interface; config picks the implementation
- [ ] Round robin · weighted round robin · least connections · power-of-two-choices
- [ ] Hot reload of the algorithm — no restart, no dropped connections
- [ ] Understand *why* P2C works before writing it
- [ ] EWMA — only if the week has room

**Milestone:** change the algorithm in YAML, hot reload, no restart. P2C
demonstrably beats round robin under deliberately uneven backend load.

## Week 6 — Observability, honest benchmarks, write-up

- [ ] Micrometer → `/metrics` → Prometheus → Grafana
- [ ] RPS, p50/p95/p99 per backend, in-flight, error rate by status class, pool health, ejection events
- [ ] `grafana/dashboard.json` committed
- [ ] Benchmarks per the rules in `BENCHMARKS.md` — read them *before* running anything
- [ ] README: architecture diagram, failover GIF, quickstart, limitations
- [ ] `docs/design.md` ADRs current; `FAILURE_MODES.md` rows filled from real runs

**Milestone:** someone who has never seen the repo understands what it does and
why it's hard in five minutes, without you in the room.

---

## ▓▓▓ SHIP LINE ▓▓▓

Stop here and it's a real project. Everything below is optional, in this order.

---

## Extensions, ranked by interview value per hour

| # | Extension | Time | Milestone |
|---|---|---|---|
| E1 | Single-flight request coalescing | ~1 wk | 10,000 concurrent requests for one cold key → exactly one line in the backend's access log. Screenshot it |
| E2 | Circuit breaker, retry budget, hedging, load shedding | ~1.5 wk | Chaos script kills and slows backends for 10 min; balancer never goes down; error rate < 1% |
| E3 | Netty rewrite + three-way thread-model shootout | ~2–3 wk | `BENCHMARKS.md` section: platform vs virtual vs event loop at 1k / 5k / 10k, flame graph, reasoning. Only with 2–3 clear weeks |
| E4 | Distributed rate limiting (Redis + Lua) | ~1 wk | Correct `429`s with accurate `Retry-After`, visible in Grafana. Fail-open vs fail-closed decided and written down |
| E5 | Full edge cache | ~1.5 wk | LRU + TTL honouring `Cache-Control` / `ETag`, stale-while-revalidate, key normalisation. Lowest payoff — last |

## Just-in-time learning

Learn each of these in the days immediately before the week needs it, with the
problem in front of you — not in advance.

| Before | Learn | Budget |
|---|---|---|
| Week 1 | Sockets, raw HTTP/1.1 on the wire. Do the `telnet example.com 80` exercise first | 1–2 days |
| Week 2 | YAML parsing, structured logging, JUnit 5 against a real port-0 server, GitHub Actions | 1–2 days |
| Week 3 | What a thread costs, what blocking means at OS level, virtual threads, **pinning** | 2–3 days |
| Week 4 | Connect vs read timeout, `ScheduledExecutorService`, safe concurrent reads of the backend list | 1–2 days |
| Week 5 | The algorithms, the P2C intuition, `AtomicInteger` / `LongAdder`, consistent hashing only if needed | 2 days |
| Week 6 | Percentiles vs averages, histograms, PromQL `rate()` / `histogram_quantile()`, open vs closed load, **coordinated omission** | 3–4 days |

Deliberately *not* learning: Kubernetes, Kafka, Spring Cloud, Raft, TLS
internals, DNS internals.
