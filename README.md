# E-Commerce Microservices Platform

A backend-focused e-commerce platform built as four independently deployable Spring Boot
services behind an API gateway, communicating synchronously over REST and asynchronously
over Apache Kafka.

Every service owns its own PostgreSQL database, ships its own Docker image, and can be
deployed and scaled on its own. The order → payment → stock flow is fully event-driven and
eventually consistent, with idempotent consumers on both sides of the exchange.

**Stack:** Java 21 · Spring Boot 4.1 · PostgreSQL 16 · Apache Kafka (KRaft) · Redis ·
Spring Cloud Gateway · JWT · Docker · Kubernetes · Prometheus · Grafana · Testcontainers ·
GitHub Actions

> The engineering decision log — including the *why* behind each choice and the mistakes made
> along the way — lives in [CONTEXT.md](CONTEXT.md).

---

## System Architecture

<img width="990" height="1578" alt="E-Commerce Microservices — System Design" src="https://github.com/user-attachments/assets/14fce462-89c4-4d54-ad33-02df2e3dbfc3" />

**Why a gateway.** Clients get one origin and one place where authentication happens. The
gateway validates the JWT signature at the edge and forwards the verified user id as
`X-User-Id`, but order-service independently validates the token as well — defence in depth,
so a service reached directly (bypassing the gateway) is still protected.

**Why no shared database.** Cross-service foreign keys are impossible by design: an order item
stores a plain `productId`, not a reference. That is the cost of service autonomy, and it is
paid deliberately.

**Why no shared event library.** `OrderPlacedEvent` is duplicated in every service that
consumes it. A shared jar would re-couple services meant to deploy independently — an upgrade
would force a lockstep release of all of them.

---

## Asynchronous Order Flow

Placing an order is a single synchronous REST call; everything after it is events.

```
POST /api/orders  (authenticated)
   │
   ├─ order-service validates products + prices over REST (product-service)
   ├─ saves the order as PENDING, snapshotting unit price
   └─ publishes OrderPlacedEvent ──> topic: order-events   (key = orderId)
          │
          ├──> payment-service   (consumer group: payment-service)
          │        ├─ idempotency check: findByOrderId — already processed? replay the result
          │        ├─ simulates authorisation
          │        └─ publishes PaymentResultEvent ──> topic: payment-results
          │                │
          │                └──> order-service (group: order-service)
          │                         └─ PENDING ──> CONFIRMED | CANCELLED
          │
          └──> product-service   (consumer group: product-service)
                   └─ decrements stock, evicts the product cache
```

Three details that make this work:

- **Two consumer groups on one topic.** `order-events` is read by both payment-service and
  product-service. Because their group ids differ, each group receives *every* message. A
  shared group id would split messages between them and each order would be half-processed.
- **The message key is the `orderId`.** Kafka guarantees ordering within a partition and routes
  by key hash, so every event about one order lands on one partition and arrives in order.
- **Consumers are idempotent because they have to be.** Kafka delivers *at-least-once*, not
  exactly-once. A redelivery must not double-charge, so `payment.order_id` carries a UNIQUE
  constraint and the consumer checks `findByOrderId` before processing — structural protection,
  not just an `if`.

Payment simulation is deterministic: a total over **1000.00** is declined. A reproducible
failure path is far more useful for demos and tests than a random one.

---

## Services

| Service | Port | Database (host port) | Responsibility |
|---|---|---|---|
| **api-gateway** | 8090 | — | Single entry point; path routing, JWT validation, CORS |
| **product-service** | 8080 | productdb (5432) | Catalog CRUD, Postgres full-text search, Redis cache; consumes `order-events` to decrement stock |
| **user-service** | 8081 | userdb (5434) | Registration, BCrypt hashing, login, JWT issuing |
| **order-service** | 8082 | orderdb (5435) | Order placement and history; publishes `order-events`, consumes `payment-results` |
| **payment-service** | 8083 | paymentdb (5436) | Consumes `order-events`, simulates payment, publishes `payment-results`. **Event-driven only — exposes no REST API** |
| kafka | 9092 host / 9093 in-network | — | KRaft mode (no ZooKeeper), dual listeners |
| redis | 6379 | — | Product read cache, 10-minute TTL, no volume (disposable by design) |
| prometheus | 9090 | — | Scrapes `/actuator/prometheus` on all four services |
| grafana | 3000 | — | Dashboards over Prometheus |

---

## API

All requests go through the gateway at `http://localhost:8090`.

### Public — no token required

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/users/register` | `{ email, password, firstName, lastName }` → 201 |
| `POST` | `/api/users/login` | → `{ accessToken, tokenType, expiresInSeconds, user }` |
| `GET` | `/api/products` | Optional `?category=Electronics` |
| `GET` | `/api/products/{id}` | Served from Redis on a cache hit |
| `GET` | `/api/products/search?q=...` | Postgres full-text, ranked by `ts_rank` |

### Authenticated — `Authorization: Bearer <token>`

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/orders` | `{ items: [{ productId, quantity }] }` → 201 `PENDING`. **No `userId` in the body** — it comes from the verified token |
| `GET` | `/api/orders` | Only ever the caller's own orders |
| `GET` | `/api/orders/{id}` | |
| `GET` | `/api/users/{id}` | |
| `POST` | `/api/products` | Catalog write |
| `PATCH` | `/api/products/{id}/stock` | PATCH, not PUT — one field, not a full replacement |
| `DELETE` | `/api/products/{id}` | → 204 |

Order status: `PENDING → CONFIRMED | CANCELLED`. Payment status: `PENDING | SUCCESS | FAILED`.

---

## Running It

**Prerequisites:** Docker Desktop, and a JDK 21 if you want to run services outside containers.
All five modules pin the same toolchain, matching the JDK used by CI and the container images.

```powershell
git clone https://github.com/ksharaff/ecommerce.git
cd ecommerce

# Build and start everything: 4 services, gateway, 4 databases, Kafka, Redis, Prometheus, Grafana
docker compose up -d --build

# Seed a 15-product catalog (registers a user, logs in, POSTs through the gateway)
.\seed-products.ps1
```

Verify the stack is healthy:

```powershell
Invoke-RestMethod http://localhost:8090/api/products | Format-Table name, price, stockQuantity
```

Watch the event flow end to end. A total over 1000.00 exercises the declined path:

```powershell
$auth = Invoke-RestMethod http://localhost:8090/api/users/login -Method Post `
  -ContentType "application/json" `
  -Body '{"email":"seed@example.com","password":"seed-password-123"}'

$headers = @{ Authorization = "Bearer $($auth.accessToken)" }

Invoke-RestMethod http://localhost:8090/api/orders -Method Post -Headers $headers `
  -ContentType "application/json" `
  -Body '{"items":[{"productId":1,"quantity":1}]}'

# Poll it: PENDING becomes CONFIRMED (or CANCELLED) once payment-service round-trips
Invoke-RestMethod http://localhost:8090/api/orders -Headers $headers
```

Follow the events as they propagate:

```powershell
docker compose logs -f payment-service product-service order-service
```

### Optional storefront

A minimal static frontend exists to exercise the API from a browser — a demo surface for the
backend, not a product.

```powershell
cd frontend
python -m http.server 3001
# → http://localhost:3001/login.html
```

Port 3001, not 3000 — Grafana's default port is 3000 and it claims that port in
`docker-compose.yml`. The gateway's CORS allowlist is pinned to `localhost:3001` to match, so
serving the storefront anywhere else means the browser blocks every API response.

---

## Observability

Prometheus scrapes `/actuator/prometheus` on all four services every 15s; Grafana reads
Prometheus. Actuator exposes `health`, `info` and `prometheus` only — never `*`, which would
publish environment variables, config properties and thread dumps.

| Tool | URL |
|---|---|
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (anonymous admin — local only) |

---

## Kubernetes

Manifests for a local Minikube cluster, applied in numeric order:

```bash
minikube start
minikube addons enable metrics-server        # required by the HPAs

# Images must be built and loaded into the cluster first (imagePullPolicy: Never)
docker compose build
minikube image load product-service:latest   # ...and one per service

kubectl apply -f k8s/
```

| File | Contents |
|---|---|
| `00-config.yaml` | ConfigMap + Secret (base64-encoded, **not** encrypted — noted deliberately) |
| `01-postgres.yaml` | Four independent Postgres deployments |
| `02-kafka-redis.yaml` | Kafka (KRaft) and Redis |
| `03-services.yaml` | The four app deployments: liveness + readiness probes, resource requests/limits, 2 replicas each |
| `04-autoscaling.yaml` | HPAs on CPU (target 60%, 2–6 replicas) with asymmetric scale-up/scale-down windows |

Demonstrates self-healing (kill a pod, watch it return), load balancing across replicas, and
horizontal autoscaling under load.

---

## Testing & CI

| Service | Unit | Controller (`@WebMvcTest` + `@MockitoBean`) | Repository (Testcontainers) |
|---|---|---|---|
| product-service | ✅ | ✅ | ✅ real Postgres 16 |
| user-service | ✅ | ✅ | — |
| order-service | ✅ | ✅ | — |
| payment-service | context-load only | — | — |

Repository tests run against a **real PostgreSQL container**, never H2 — dialect differences
mean an H2-passing test is not proof the query works in production, and the full-text search
query is native Postgres SQL that H2 cannot execute at all.

[GitHub Actions](.github/workflows/ci.yml) runs the four test suites as a parallel matrix with
`fail-fast: false`, so one broken service does not hide the state of the other three. Test
reports upload as artifacts even when the run fails.

```powershell
cd product-service; .\gradlew test
```

---

## Engineering Notes

Details chosen deliberately, and worth a conversation:

- **`price` is `BigDecimal`, compared with `.compareTo()`** — binary floating point cannot
  represent decimal money exactly, and `BigDecimal.equals()` considers `10.0` and `10.00`
  different values.
- **`stockQuantity` is `Integer`, not `int`** — a primitive cannot be null, which would silently
  disguise missing data as a legitimate `0`.
- **`unitPriceAtOrderTime` is a price snapshot** — a later catalog price change must not rewrite
  what a customer was historically charged.
- **Login cannot enumerate accounts** — unknown email and wrong password throw the *same*
  exception, producing an identical 401. Differing responses would let an attacker discover
  which emails are registered.
- **Email uniqueness is enforced at the database level**, not only in code: two simultaneous
  signups can both pass an application-level check before either commits.
- **The JWT is signed, not encrypted.** It carries `sub` (userId) and `email` only; anyone
  holding it can read every claim, so nothing sensitive goes in it.
- **Cache invalidation is explicit** — `@CacheEvict` on every mutation, including
  `allEntries = true` on bulk stock decrements, backed by a 10-minute TTL as a safety net so a
  missed eviction self-corrects instead of serving stale data forever.
- **Search is deliberately not cached** — search terms are unbounded, so caching them would fill
  Redis with keys that are never read twice.
- **Constructor injection only**, no field `@Autowired`; entities never leave the service layer;
  one custom exception per failure case, translated to status codes by `@RestControllerAdvice`.

---

## Known Limitations

Deliberate scope boundaries, listed because knowing what is missing matters as much as what is built:

- **No transactional outbox.** The order is saved and *then* the event is published; a crash
  between the two leaves an order with no event. The correct fix is writing the event and the
  order in one transaction and relaying it separately.
- **No saga / compensation.** Stock is decremented on `OrderPlaced` rather than on payment
  success, so a declined payment leaves stock wrongly reduced. Real systems reserve stock and
  emit a compensating release event.
- **Insufficient stock at decrement time is logged and skipped**, not compensated — an honest
  consequence of eventual consistency without a reservation step.
- **No roles or authorisation.** Authentication is enforced, but any authenticated user can
  write to the catalog. There is no admin/customer distinction yet.
- **No refresh tokens.** A JWT cannot be revoked before it expires; logout is client-side only.
- **No pagination.** List endpoints return everything. Correct at 15 products, wrong at 15,000.
- **`imagePullPolicy: Never`** in the k8s manifests is a Minikube shortcut; real deployments
  pull tagged images from a registry.
- **Kubernetes is local-only** (Minikube). Cloud deployment is explicitly out of scope.

---

## Repository Layout

```
├── api-gateway/          Spring Cloud Gateway — routing, JWT filter, CORS
├── product-service/      Catalog, search, Redis cache, stock consumer
├── user-service/         Registration, login, JWT issuing
├── order-service/        Order placement, event publisher, payment-result consumer
├── payment-service/      Kafka-only payment simulation with idempotency
├── frontend/             Static demo storefront (backend exercise surface)
├── k8s/                  Minikube manifests: config, databases, services, HPAs
├── .github/workflows/    CI — parallel test matrix
├── docker-compose.yml    Full local stack
├── prometheus.yml        Scrape configuration
├── seed-products.ps1     Catalog seeding through the gateway
└── CONTEXT.md            Decision log and engineering notes
```
