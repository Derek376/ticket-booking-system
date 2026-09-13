# Development plan

Each stage is a separate commit with tests for the behavior it adds. Stage 1 is complete; stages 2–22 are planned.

## Design constraints

- One repository; a modular Spring Boot backend, a small frontend, and local infrastructure.
- Reserved-seat events, one currency initially, and simulated payments.
- At most one active owner per performance seat; multi-seat allocation is all-or-nothing.
- A customer hold lasts five minutes in durable database state. A Redis operation lock lasts only for a short operation.
- All allocation, confirmation, cancellation, and expiry paths follow one documented database locking protocol.
- PostgreSQL validates current ownership even if Redis is stale or a lock lease expires.
- Payment calls never run inside long-lived database transactions.
- Committed events survive broker outages through an outbox; consumers tolerate duplicate delivery.
- Frontend availability and countdowns are advisory. The server decides whether a booking is valid.
- Use simulated payments and notifications for local development.

## Stages

| Stage | Commit | Acceptance criteria |
| --- | --- | --- |
| 01 | Set up Spring Boot backend | Java 21, Maven wrapper, HTTP health endpoint, startup test, setup documentation. App builds and responds over HTTP. |
| 02 | Add PostgreSQL and database migrations | Local PostgreSQL configuration, migrations, application connection, Testcontainers database test. A fresh database initializes predictably. Add CI for required tests; missing Docker must not silently skip them. |
| 03 | Add venue and event APIs | Venues, seats, events, performances, validation, consistent API errors, pagination, sample data. Catalog requests persist and retrieve real data. Management endpoints are local-development only until stage 04 secures them. |
| 04 | Add login and admin permissions | Registration, login, password hashing, customer/admin roles, secured management endpoints. Choose and document the browser authentication approach with its CSRF/CORS requirements. Unauthorized actions fail in tests. |
| 05 | Add seat inventory for performances | Unique inventory per performance and seat, fixed-precision pricing, availability API, appropriate constraints. Duplicate inventory and invalid prices are rejected. |
| 06 | Prevent conflicting seat reservations | Single- and multi-seat reservations, customer ownership, ordered locks, short transactions. A concurrent single-seat test has one winner; partial allocation rolls back. |
| 07 | Expire and cancel reservations | Durable expiry worker and customer cancellation, consistent state transitions, injected clock where useful. Expired holds cannot be used even when cleanup is delayed. Races cannot release a later owner's seats. |
| 08 | Handle repeated reservation requests | Durable request keys scoped by customer and operation, payload fingerprints, stored outcomes. Concurrent retries return one logical reservation; changed payloads are rejected. Extend this mechanism to checkout in stage 09. |
| 09 | Add checkout with simulated payments | Payment attempts, idempotent checkout/callbacks, server-side amounts, atomic confirmation, price snapshots, compensation for late success, reconciliation for unknown outcomes. Confirmation-versus-expiry races have one valid result. |
| 10 | Cache event listings in Redis | Local Redis, bounded cache lifetime, invalidation, database fallback, cache integration tests. Catalog changes become visible according to a documented policy. |
| 11 | Add Redis locks for seat reservations | Bounded acquisition, ownership tokens, safe release, documented lease policy, two API instances. Expired leases and Redis failure preserve database invariants. Keep database-only mode for comparison. |
| 12 | Publish booking events to Kafka | Local Kafka, outbox rows written with business transactions, publisher, stable event IDs and schemas. Bookings survive broker outages; pending events publish after recovery. Define publication ownership and ordering for concurrent publishers. |
| 13 | Generate tickets from booking events | Worker mode, ticket records, simulated notifications, deduplication committed with effects. Replaying an event does not issue another ticket. |
| 14 | Handle failed events and retries | Bounded retries, dead-letter handling, replay instructions, stale-event handling. A failed message can be diagnosed and replayed without duplicate effects. |
| 15 | Test concurrent bookings and recovery | Expand existing tests into a failure matrix: overlapping seat groups, two instances, stale lock holder, expiry/confirmation, lost responses, duplicate callbacks, broker outages, publisher/consumer crash windows. Persisted state proves the invariants. |
| 16 | Add booking metrics and dashboards | Correlated logs, latency and conflict metrics, outbox age, consumer lag, dashboard, health/readiness policy. Sensitive data stays out of logs; operational endpoints have deliberate exposure. |
| 17 | Benchmark booking and catalog requests | Normal and burst workloads, uncached/cached and database-only/Redis comparisons, documented hardware and percentiles. Separate business conflicts from unexpected failures; tune only from evidence. |
| 18 | Add login and event browsing pages | Select a small frontend stack, implement accessible sign-in and event pages, connect to real APIs, document local startup. Verify authentication and browse flow in the browser. |
| 19 | Add seat selection and checkout pages | Seat map, reservation creation, server-based countdown, conflict recovery, idempotent checkout. Refreshes and retries do not create extra bookings. |
| 20 | Show bookings and tickets | Customer-owned booking list/details, pending issuance, tickets, useful error and empty states. Full customer flow works in the browser. |
| 21 | Package the app for deployment | App containers, complete local stack, seed data, CI builds, documented secrets and shutdown, deployment instructions. A fresh checkout can run the demo. Publish a hosted instance only when requested. |
| 22 | Document architecture and benchmark results | Architecture/ER diagrams, API reference, decision records, benchmark report, demo script, known limitations, résumé bullets based on measured results. Every claim links to evidence. |
