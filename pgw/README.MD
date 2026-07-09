# tl-paymentgateway (pgw)

A Payment gateway service that sits between a merchant (e.g. an e-commerce checkout) and a downstream bank/acquirer API. It exposes REST endpoints for the core payment lifecycle — authorize, capture, void, and refund — and persists payment state, events, and receipts.

## Features

- **Authorize** a card payment for an order and customer
- **Capture** a previously authorized payment
- **Void** an authorized (uncaptured) payment
- **Refund** a captured payment
- **Receipts** generation and lookup by order
- **Order/status lookup** by customer ID or order ID
- **Idempotency** on all bank-facing operations via a UUID derived from the request's key fields, checked against stored payment events before calling the bank, so retried requests don't double-charge or double-process
- **Payment state** tracked via a `State` enum (`PENDING`, `APPROVED`, `CAPTURED`, `VOIDED`, `REFUNDED`, `FAILED`) with explicit transitions applied per operation. (A formal `StateMachine` class exists in `state/` but isn't wired in yet — see `TRADEOFFS.md`.)
- **Bank client** (`BankClient`) that calls a separate bank/acquirer service over HTTP and distinguishes transient (`BankNotConnectingException` — including connection failures, not just 5xx responses) vs. permanent (`BankPermanentError`) failures, with retry-with-backoff on transient errors
- **OpenAPI / Swagger UI** for interactive API docs
- **Global exception handling** for consistent error responses

## Tech Stack

- Java 17
- Spring Boot 3.3.5 (Web, JDBC, Actuator, DevTools)
- H2 in-memory database (runtime) — Postgres driver is on the classpath but not yet wired up as the active datasource
- springdoc-openapi for Swagger UI
- Java's built-in `HttpClient` for outbound calls to the bank service
- Maven (with included Maven Wrapper)
- Docker / Docker Compose for local orchestration with a mock bank + its own Postgres

## Project Structure

```
pgw/
├── src/main/java/com/gateway/
│   ├── pgw/                   # Application entry point (PgwApplication)
│   ├── gateway/                # REST controller (GatewayController) + outbound BankClient + PaymentService
│   ├── bankconnectresponses/   # DTOs for bank API responses (auth, capture, refund, void)
│   ├── models/                 # Domain models (OrderDetail, PaymentDetail, PaymentEvent)
│   ├── data/                   # Repositories (Payment, PaymentEvent, Audit)
│   ├── state/                  # Payment state (State, Event, StateMachine — StateMachine currently unused)
│   ├── proof/                  # Receipt generation
│   └── errors/                 # Custom exceptions + GlobalExceptionHandler
├── src/main/resources/
│   ├── application.properties
│   └── data.sql                # Schema for payments, paymentevent, logs tables
├── docker-compose.yml           # pgw + mockbank-api + mockbank-postgres on a shared network
├── Dockerfile
└── src/test/java/...             # Spring Boot test bootstrap
```

## Prerequisites

- Java 17+ (only needed if running via Maven directly)
- Docker + Docker Compose (recommended — see [Getting Started](#getting-started))
- A running bank/acquirer service that implements the expected endpoints (see [Bank Dependency](#bank-dependency) below)

## Getting Started

### Option A: Docker Compose (recommended)

Brings up the gateway, a mock bank API, and its Postgres in one command, all on a shared network so the gateway can reach the bank by service name:

```bash
git clone https://github.com/ifaycodes/tl-paymentgateway.git
cd tl-paymentgateway/pgw
docker compose up -d --build
```

The gateway is then reachable at `http://localhost:8081`. `BANK_API_BASE_URL` is set for you inside `docker-compose.yml` (`http://mockbank-api:8080/api/v1`) — no manual bank setup required.

### Option B: Docker (standalone)

If you're running the gateway container by itself against a bank that's already reachable elsewhere, remember to publish the port — `EXPOSE` in the Dockerfile only documents the port, it doesn't publish it:

```bash
docker build -t pgw:v1 .
docker run -d -p 8081:8081 -e BANK_API_BASE_URL=http://<your-bank-host>:8787/api/v1 --name pgw pgw:v1
```

### Option C: Maven

```bash
./mvnw spring-boot:run
```

Requires a bank/acquirer service already running and reachable at `http://localhost:8787/api/v1` (override via `BANK_API_BASE_URL` env var if it's elsewhere).

---

The app starts on **port 8081** and uses an in-memory H2 database seeded from `data.sql` on each run — data does not persist across restarts.

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI spec: `http://localhost:8081/api-docs`
- H2 console: `http://localhost:8081/h2-console` — JDBC URL `jdbc:h2:mem:testdb`, username `postgres`, password `ugwonna` (these are placeholder H2 credentials from `application.properties`, not real Postgres credentials — the app isn't running on Postgres yet)
- Actuator: `http://localhost:8081/actuator/health` (only the health endpoint is exposed by default; `management.endpoints.web.exposure.include` isn't set, so other actuator endpoints aren't reachable)

## Bank Dependency

`BankClient` calls a bank/acquirer service at `bank.api.base-url` (`application.properties`), overridable via the `BANK_API_BASE_URL` env var, expecting endpoints for `/authorizations`, `/captures`, `/voids`, and `/refunds`.

When running via `docker compose up`, this is handled for you — `docker-compose.yml` starts [`mockbank-api`](https://github.com/benx421/payment-gateway/blob/main/bank/README.md) (a Go-based mock bank simulator) with its own Postgres, on the same Docker network as `pgw`, and points `BANK_API_BASE_URL` at `http://mockbank-api:8080/api/v1`. When running the gateway outside Docker (or as a standalone container), you're responsible for having an equivalent bank service reachable at that URL.

## API Overview

All endpoints are under `/api/v1`.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth` | Authorize a card payment. Body: `orderId`, `customerId`, `amt`, `cardNumber`, `cvv`, `expiryMonth`, `expiryYear` |
| GET | `/getauth?paymentRef=...` | Fetch authorization detail from the bank for this payment ref |
| POST | `/capt` | Capture a previously authorized payment. Body: raw `paymentRef` string |
| GET | `/getcapt?paymentRef=...` | Fetch capture detail from the bank for this payment ref |
| POST | `/void` | Void an authorized payment. Body: raw `paymentRef` string |
| POST | `/ref` | Refund a captured payment. Body: raw `paymentRef` string |
| GET | `/getref?paymentRef=...` | Fetch refund detail from the bank for this payment ref |
| POST | `/receipts/{orderId}` | Generate a receipt for an order |
| GET | `/orders?customerId=...` | Get payment details for a customer |
| GET | `/status?orderId=...` | Get payment status for an order |

Every mutating operation (authorize, capture, void, refund) generates an idempotency key from relevant request fields, checks it against stored payment events, and skips re-processing if it has already been handled.

## Configuration

Key settings in `src/main/resources/application.properties`:

```properties
server.port=8081
spring.datasource.url=jdbc:h2:mem:testdb
spring.sql.init.mode=always
springdoc.swagger-ui.path=/swagger-ui.html
bank.api.base-url=${BANK_API_BASE_URL:http://localhost:8787/api/v1}
```

`bank.api.base-url` is overridable via the `BANK_API_BASE_URL` environment variable — this is how `docker-compose.yml` points the gateway at the `mockbank-api` service instead of `localhost`.

## Status

This is a project exploring payment gateway architecture: state machines, idempotency, and integration with an external bank API — treat it as a reference implementation rather than a deployable payment system.

Note: the test suite (`BankTest`) makes real HTTP calls to a live bank service, so it's skipped during `docker build` (`mvn clean package -DskipTests`) since the build stage has no network route to a bank. Run tests locally with `./mvnw test` against a running mock bank instead.
