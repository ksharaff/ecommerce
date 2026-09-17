# CONTEXT.md — E-Commerce Microservices Platform

Living record of decisions and progress. Lives at `ecommerce\CONTEXT.md` (repo root).
Fastest way to re-orient yourself — or anyone else, including a future AI session — on where things stand.

## Project Overview

- **Goal:** learn backend engineering hands-on by building a portfolio-grade microservices e-commerce backend, written manually (not scaffolded by an AI coding agent) so the learning actually sticks.
- **Audience:** backend/software engineering job search, on the Java/Spring Boot track.
- **Pace:** a few hours a week, ongoing.
- **Learning style:** total-beginner treatment throughout — every design decision explained with its "why."

## Tech Stack & Key Decisions

| Decision | Choice | Why |
|---|---|---|
| Language / framework | Java + Spring Boot, all services | Matches resume; Spring's ecosystem covers nearly the whole target stack natively |
| Build tool | Gradle | Matches prior experience — kept constant while everything else was new |
| Database | PostgreSQL, one instance per service | Enforces "each service owns its data" — no cross-service DB access, ever |
| Event broker | Apache Kafka, **KRaft mode** (no ZooKeeper) | ZooKeeper obsolete since Kafka 3.3+; one less container |
| Cache | Redis (product reads only) | Hot, rarely-changing data. Deliberately not caching search — unbounded keys |
| Search | Postgres full-text (`to_tsvector`/`ts_rank`) | Real ranking and stemming without a second data store to keep in sync. Elasticsearch would be over-engineering at this scale |
| Auth | JWT (HMAC-SHA256), stateless | No shared session store needed across services/replicas |
| Gateway | Spring Cloud Gateway (WebFlux) | Single entry point; JWT enforced at the edge |
| Containerization | Docker, multi-stage builds (JDK build → JRE runtime) | Smaller images; no compiler in the runtime container |
| Orchestration | Kubernetes — **local only** (Minikube) | Free; still demonstrates the pattern. Cloud deployment explicitly out of scope |
| Repo structure | Monorepo — one folder per service | Easiest to manage solo; each service still ships its own image |
| Event classes | **Duplicated per service**, not a shared library | A shared library re-couples services meant to deploy independently |

## Service Map

| Service | Port | Database (host port) | Role |
|---|---|---|---|
| product-service | 8080 | productdb (5432) | Catalog CRUD, search, Redis cache; consumes `order-events` to decrement stock |
| user-service | 8081 | userdb (5434) | Registration, login, BCrypt hashing, JWT issuing |
| order-service | 8082 | orderdb (5435) | Places orders; publishes `order-events`, consumes `payment-results`; validates JWTs |
| payment-service | 8083 | paymentdb (5436) | Consumes `order-events`, simulates payment, publishes `payment-results` |
| api-gateway | 8090 | — | Single entry point; routes by path, validates JWT, handles CORS |
| kafka | 9092 (host) / 9093 (docker) | — | KRaft mode, dual listeners |
| redis | 6379 | — | Product read cache (no volume — disposable by design) |
| prometheus | 9090 | — | Scrapes `/actuator/prometheus` on all four Spring services |
| grafana | 3000 | — | Dashboards over Prometheus; anonymous admin (local-only) |
| frontend | 3001 | — | Static `login.html` + `index.html`, served separately (3001 because Grafana owns 3000) |

## Event Flow (working end to end)

```
Order placed (REST, authenticated)
   └─> order-service saves order as PENDING
        └─> publishes OrderPlacedEvent → topic: order-events
             ├─> payment-service   (group: payment-service)  → simulates payment
             │      └─> publishes PaymentResultEvent → topic: payment-results
             │           └─> order-service → CONFIRMED or CANCELLED
             └─> product-service   (group: product-service)  → decrements stock
```

- **Two consumer groups on one topic** is the key mechanism: each group gets its own copy of every message. Same group id would split messages between them instead.
- **Message key = orderId** — Kafka guarantees ordering within a partition and routes by key, so events about one order always arrive in order.
- **Payment simulation is deterministic:** total > 1000.00 is declined. Reproducible failure path on demand, better than a coin flip for demos.

## Phase Status

| Phase | Status |
|---|---|
| 0 — Product service (layers, tests, Docker) | ✅ Complete |
| 1 — User + Order services, REST inter-service calls | ✅ Complete |
| 2 — Kafka event flow + Payment service | ✅ Complete |
| 3 — Redis caching + Postgres full-text search | ✅ Complete |
| 4 — Prometheus + Grafana observability | ✅ Complete |
| 5 — Kubernetes on Minikube (probes, HPA, self-healing) | ✅ Complete |
| 6 — GitHub Actions CI (tests) | 🚧 Workflow written; image build/push to registry not done |
| JWT authentication | ✅ Complete |
| API Gateway | ✅ Complete |
| 7 — Frontend storefront | ✅ Complete |

## Key Implementation Notes

**Product service** — `price` is `BigDecimal` (binary floating point can't represent decimals exactly); `stockQuantity` is `Integer` not `int` (a primitive can't be null, hiding missing data as a silent 0). `@Cacheable` on `getProduct`, `@CacheEvict` on every mutation — including `allEntries = true` on the bulk stock decrement. Full-text search via native query with `ts_rank` ordering.

**User service** — `email` unique at the DB level (a code-only check has a race condition on simultaneous signups); field named `passwordHash`, never `password`. BCrypt via standalone `spring-security-crypto`, deliberately not full `spring-boot-starter-security`. Unknown email and wrong password throw the **same** exception → identical 401, so login can't enumerate accounts.

**Order service** — `userId`/`productId` are plain `Long`s: no foreign key is possible across separate databases. `unitPriceAtOrderTime` is a **price snapshot**, so later price changes don't rewrite history. Two distinct client failure types: `*NotFoundException` (doesn't exist → 400) vs `ServiceUnavailableException` (couldn't reach the service → 503).

**Payment service** — `orderId` is **unique**, structural protection against double-charging. Idempotency check via `findByOrderId` before processing: Kafka guarantees **at-least-once** delivery, not exactly-once, so consumers must be idempotent.

**JWT** — HMAC-SHA256, shared secret identical across user-service, order-service, and gateway. Token carries `sub` (userId) and `email` only; it is signed, **not encrypted**, and anyone holding it can read every claim. `CreateOrderRequest` has **no** `userId` field — it comes from the verified token, so a caller cannot order as someone else.

**Gateway** — path-based routing, `GlobalFilter` validating JWTs at the edge, public allowlist for register/login and `GET /api/products`. Forwards `X-User-Id` downstream, but order-service still validates the token itself (defense in depth).

## Gotchas Hit (worth remembering)

- **Kubernetes auto-injects `<SERVICE>_PORT` env vars** for every Service in the namespace, as a URL like `tcp://10.x.x.x:6379`. A Service named `redis` silently clobbered `${REDIS_PORT:6379}` and crashed product-service with a number-format error. **Never name config env vars after Kubernetes Service names.**
- **`minikube docker-env` fails with the containerd runtime** on Windows. Use `minikube image load <image>` instead — works with any runtime and driver.
- **Three-step cycle after any code change in Kubernetes:** rebuild image → `minikube image load` → `kubectl rollout restart`. Skipping the middle step silently keeps the old image running.
- **`CrashLoopBackOff` + Hibernate "Unable to determine Dialect"** almost always means the database isn't reachable, not a JPA config problem. The dialect message describes the second failure, not the first.
- **Spring `@Valid` doesn't recurse into a nested list** without `@Valid` on the list field itself — inner validation annotations silently never run.
- **Spring relaxed binding:** property → env var is dots to underscores, **dashes removed**, uppercased. `services.product-service.base-url` → `SERVICES_PRODUCTSERVICE_BASEURL`.
- **PowerShell:** no `\` line continuation (use backtick or one line); `Invoke-RestMethod` throws on non-2xx and hides the body — use `try/catch` + `$_.ErrorDetails.Message`; `ConvertTo-Json -Depth 3` for nested payloads.
- **`kubectl port-forward` prints two lines and then goes silent** — that's success, not a hang. It needs its own terminal, like `bootRun`.

## Known Gaps (deliberate — be ready to explain these)

- **No transactional outbox.** Order publishes after saving; a crash in between means an order with no event. Correct fix: write event and order in one transaction, relay separately.
- **Stock decrements on `OrderPlaced`, not on payment success.** If payment fails, stock stays wrongly reduced. Real systems use a reservation step plus a compensating "release stock" event (the **saga** pattern).
- **Insufficient stock at decrement time is logged and skipped**, not compensated — a genuine consequence of eventual consistency.
- **No refresh tokens.** A JWT can't be revoked; logout is client-side only. Standard mitigation is short-lived access tokens plus a revocable refresh token.
- **Frontend filtering and sorting are client-side.** Correct at 15 products, wrong at 15,000 — that must move server-side as `?sort=&page=&size=`.
- **Token in `sessionStorage`** — readable by any script, so XSS means session theft. Production answer is an httpOnly cookie plus CSRF protection.
- **`imagePullPolicy: Never` in the k8s manifests** — a local-dev shortcut. Real deployments pull tagged images from a registry, which is what finishing CI would provide.
- **payment-service has only the generated context-load test** (`PaymentServiceApplicationTests`) — the only service without a real test pyramid, and it holds the most interesting logic (the idempotency check). Biggest remaining gap.

## Conventions

- Constructor injection only — never field-level `@Autowired`
- No interface+impl split for services — unneeded layering for a single implementation
- Entities never leave the service layer; controllers only see/return DTOs
- Each service owns its data — no cross-service database access
- One custom exception per failure case, translated via `@RestControllerAdvice`
- **Testcontainers** (real Postgres) for repository tests, never H2 — dialect differences make H2-passing tests unreliable proof. Currently honoured only in `ProductRepositoryTest`, the one repository test that exists; stale `com.h2database:h2` test dependencies still linger in product-service and user-service `build.gradle` and should be removed
- `@WebMvcTest` + `@MockitoBean` for controller tests (not the deprecated `@MockBean`)
- `BigDecimal.compareTo`, never `.equals()` — equals() considers 10.0 and 10.00 different
- **Container networking:** services reach each other by *service name* (`kafka:9093`, `product-db:5432`), never `localhost`. Host ports are only for reaching in from your own machine.
- Frontend: escape all server-supplied text before `innerHTML` — a product named `<img onerror=...>` would otherwise execute

## Running It

```powershell
# Everything
cd C:\Users\khash\Documents\DEV\ecommerce
docker compose up -d --build

# Seed a realistic catalog
.\seed-products.ps1

# Serve the storefront
cd frontend
python -m http.server 3001
# → http://localhost:3001/login.html
```

**The storefront runs on 3001, not 3000.** Grafana's default port is 3000 and it takes that port in
`docker-compose.yml`, so the two used to collide whenever the full stack was up. Grafana has a
convention behind its port and a static file server does not, so the frontend moved. Moving it
required changing the gateway too: `CorsConfig` allowlists origins explicitly, so an unlisted
origin gets every response blocked by the browser regardless of what the server returns.

Kubernetes deployment: apply the manifests in `k8s/` in numeric order (`00-config` → `04-autoscaling`).
Each file is commented inline; there is no separate `k8s/README.md`.

## Remaining / Optional

- payment-service test pyramid *(highest value)*
- Finish CI: build + push images to GHCR, then drop `imagePullPolicy: Never`
- README with architecture diagram + demo recordings (self-healing pod, HPA scaling under load)
- Swagger/OpenAPI per service (`springdoc-openapi-starter-webmvc-ui`) — quick win
- Refresh tokens; server-side pagination and sorting
- Set `spring.application.name=product-service` — it is still the generated `demo`, so every
  Prometheus metric from that service is tagged `application="demo"`
