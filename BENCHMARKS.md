# Benchmarks

> **No measurements have been taken yet.** This file exists from day 0 so the
> methodology is decided *before* the first load test, not rationalised after it.
> Nothing under "Results" is real until it says otherwise.

## Rules

1. **Topology before numbers.** Every result is preceded by what ran where.
2. **Two figures, always labelled.**
   - **Proxy overhead** — p50/p99 of (time through Switchyard) − (time direct to
     origin), measured against a static responder that returns a fixed 200, with
     the load generator on a *different machine*. This is the number that says
     whether the balancer is any good. Target: sub-millisecond to low single-digit
     milliseconds. nginx does this in the hundreds of microseconds.
   - **End-to-end p99 under load** — includes backend processing time.
     Legitimately larger. Fine to report, as long as it's labelled as this one.
3. **Fixed-rate load, never closed-loop.** A generator that waits for each
   response before sending the next stops sending during slowdowns and therefore
   never records them — the worst latencies vanish from the data. This is
   *coordinated omission*. Use a tool that sends at a fixed rate and corrects for
   it. Read Gil Tene on this before running anything.
4. **Load generator on a separate host.** Never on the same box as the thing
   being measured. Seven processes fighting over four cores is not a benchmark.
5. **Pause Prometheus scraping** during measurement runs, or state its cost.
6. **Find the breaking point** and name the bottleneck one layer deeper than the
   number: CPU, GC pause, connection-pool exhaustion, file-descriptor limit.
7. **Warm up first.** State warmup and run durations.
8. **No figure leaves this file** — for the README or a resume — until it has
   been reproduced on a clean setup.

## Topology

| Component | Host | Cores / RAM | Notes |
|---|---|---|---|
| Switchyard | — | — | |
| Backends (×3) | — | — | Static responder for overhead runs; real backends for e2e |
| Load generator | — | — | **Must be a different machine** |
| Prometheus / Grafana | — | — | Paused during runs? |

Tool and version: —
Connections: — · Rate: — · Warmup: — · Duration: —

## Results

_None yet._

### Proxy overhead (vs static origin)

| Metric | Direct | Via Switchyard | Overhead |
|---|---|---|---|
| p50 | — | — | — |
| p99 | — | — | — |

### End-to-end under load

| RPS | p50 | p95 | p99 | Error rate | Notes |
|---|---|---|---|---|---|
| — | — | — | — | — | |

### Breaking point

_At ~— req/s, p99 degrades because —._

## The honest paragraph

Template. Every blank is filled from a real run, never guessed:

> These figures come from a —-core — instance with backends on —; that
> co-residency [does / does not] inflate end-to-end p99 under load. Proxy
> overhead was measured separately, against a static origin, with the load
> generator on a different machine. The honest ceiling on this hardware is
> ~— req/s, at which point the bottleneck is —. On dedicated hardware I'd
> expect —.
