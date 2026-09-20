# Failure modes

What breaks, how Switchyard notices, how it behaves *while* broken, and how it
recovers. Every row here should be something actually induced and observed —
a `docker stop`, a `?delay=`, a `kill -9` — not reasoned about from the
armchair. Rows fill in as the mechanism that handles them is built; until then
they are marked open.

Reading this file should feel like reading experience.

## Legend

- **Detected by** — the mechanism that notices (active probe, passive tracker,
  timeout, circuit breaker…) and the *observed* time to detect
- **While broken** — what a client sees during the window
- **Recovery** — what has to happen for traffic to return, and how it ramps

## Backend failures

| Failure | Detected by | While broken | Recovery | Status |
|---|---|---|---|---|
| Backend process dies cleanly (`docker stop`) | — | — | — | open (week 4) |
| Backend dies mid-request — after receiving, before answering | — | — | — | open (week 4) |
| Backend hangs — accepts connections, never responds | — | — | — | open (week 4) |
| Backend slow (`?delay=`) but its `/health` says fine | — | — | — | open (week 4, passive) |
| Backend returns a burst of 5xx | — | — | — | open (week 4 / E2) |
| Backend recovers after ejection | — | — | — | open (week 4, slow-start) |
| Every backend in a pool is down | — | — | — | open |

## Balancer-side failures

| Failure | Detected by | While broken | Recovery | Status |
|---|---|---|---|---|
| Config reload with malformed YAML | — | — | — | open (week 2 / 5) |
| Config reload removes a backend with in-flight requests | — | — | — | open (week 5) |
| Connection pool to a backend exhausted | — | — | — | open (week 6 bench) |
| File-descriptor limit hit under load | — | — | — | open (week 3 / 6) |
| GC pause during load | — | — | — | open (week 6 bench) |
| Retry storm — retries amplify a slowdown into an outage | — | — | — | open (E2, retry budget) |
| Thundering herd on a cold cache key | — | — | — | open (E1) |

## Dependency failures

| Failure | Detected by | While broken | Recovery | Status |
|---|---|---|---|---|
| Redis unreachable (rate limiter) — fail open or fail closed? | — | — | — | open (E4; decision goes in design.md) |
| Prometheus scrape landing inside a benchmark run | — | — | — | open (week 6) |

## Not handled, on purpose

- **The balancer instance itself dying.** Single instance, no HA. Documented
  limitation; the fix is a second instance behind DNS or a VIP, which is out of scope.
- **Retrying non-idempotent requests.** Never. A `POST` that timed out may or may
  not have been processed by the backend; retrying could charge a card twice.
  Timeouts on non-idempotent requests surface to the client as errors.
