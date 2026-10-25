# Switchyard

[![CI](https://github.com/sizzywizzy/SwitchYard/actions/workflows/ci.yml/badge.svg)](https://github.com/sizzywizzy/SwitchYard/actions/workflows/ci.yml)

A Layer 7 HTTP load balancer, written from scratch in Java.

> **Status: day 0.** Repository skeleton only — no code yet. The plan is in
> [`docs/roadmap.md`](docs/roadmap.md). This README fills in as each milestone
> is actually reached, not before.

## What it does

Switchyard sits between clients and a pool of backend servers. For every incoming
HTTP request it accepts the connection, parses the request, chooses one healthy
backend, forwards the request over a pooled connection, and streams the response
back — while keeping track of which backends are slow, dead, or recovering.

*Layer 7* means it reads the HTTP itself (path, method, headers) rather than just
shovelling bytes, so it can route `/api/*` and `/static/*` to different pools.

## Architecture

```
client → Listener → Router → Rate limit → Cache → Balancer → Circuit breaker → Proxy → backend
```

Each stage is a filter that can be switched on or off in config. The full
breakdown — what each stage does and what breaks without it — is in
[`docs/design.md`](docs/design.md#architecture-overview).

<!-- TODO (week 6): real diagram + failover GIF -->

## Quickstart

<!-- TODO (week 2): `docker compose up`, then curl. Not runnable yet. -->

Not runnable yet.

## Live instance

<!-- TODO (week 2): URL + public Grafana. A dead link is worse than no link,
     so nothing goes here until it is actually up and stays up. -->

## Benchmarks

No measurements yet. When there are, they live in [`BENCHMARKS.md`](BENCHMARKS.md)
with the topology stated *before* the numbers. Two figures will be reported and
labelled separately: **proxy overhead** (through Switchyard minus direct to origin,
against a static responder, load generator on a separate host) and **end-to-end
p99 under load** (includes backend time).

## Design decisions

Every real decision, the alternatives rejected, and what would change at 100×
scale: [`docs/design.md`](docs/design.md).

## Failure modes

What breaks, how it's detected, and how the system behaves while broken:
[`FAILURE_MODES.md`](FAILURE_MODES.md).

## Limitations

Kept honest and current.

- Single balancer instance — no leader election, no HA for the balancer itself
- HTTP/1.1 only
- TLS is terminated in front of Switchyard (Caddy), not by it
