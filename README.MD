# tl-paymentgateway (pgw)

A Payment gateway service that sits between a merchant (e.g. an e-commerce checkout) and a downstream bank/acquirer API. It exposes REST endpoints for the core payment lifecycle — authorize, capture, void, and refund — and persists payment state, events, and receipts.

## Features

- **Authorize** a card payment for an order and customer
- **Capture** a previously authorized payment
- **Void** an authorized (uncaptured) payment
- **Refund** a captured payment
- **Receipts** generation and lookup by order
- **Order/status lookup** by customer ID or order ID
- **Idempotency** on all bank-facing operations via a SHA-based idempotency key, so retried requests don't double-charge or double-process
- **Payment state machine** (`State`, `StateMachine`) tracking transitions such as `PENDING → APPROVED → CAPTURED/VOIDED/REFUNDED`
- **Bank client** (`BankClient`) that calls a separate bank/acquirer service over HTTP and distinguishes transient (`BankNotConnectingException`) vs. permanent (`BankPermanentError`) failures
- **OpenAPI / Swagger UI** for interactive API docs
- **Global exception handling** for consistent error responses

## Tech Stack

- Java 17
- Spring Boot 3.3.5 (Web, JDBC, Actuator, DevTools)
- H2 in-memory database (runtime)
- springdoc-openapi for Swagger UI
- Java's built-in `HttpClient` for outbound calls to the bank service
- Maven (with included Maven Wrapper)

## Project Structure

```
pgw/
├── src/main/java/com/gateway/
│   ├── pgw/                   # Application entry point (PgwApplication)
│   ├── gateway/                # REST controller (GatewayApis) + outbound BankClient
│   ├── bankconnectresponses/   # DTOs for bank API responses (auth, capture, refund, void)
│   ├── models/                 # Domain models (CardDetail, PaymentDetail, PaymentEvent, ReceiptDetails)
│   ├── data/                   # Repositories (Payment, PaymentEvent, Receipt)
│   ├── state/                  # Payment state machine (State, Event, StateMachine)
│   ├── proof/                  # Receipt generation
│   └── errors/                 # Custom exceptions + GlobalExceptionHandler
├── src/main/resources/
│   ├── application.properties
│   └── data.sql                # Schema for payments, paymentevent, auditLog tables
└── src/test/java/...            # Spring Boot test bootstrap
```

## Prerequisites

- Java 17+
- A running bank/acquirer service that implements the expected endpoints (see [Bank Dependency](#bank-dependency) below)

## Getting Started

Clone the repo and run the app from the `pgw` directory:

```bash
git clone https://github.com/ifaycodes/tl-paymentgateway.git
cd tl-paymentgateway/pgw
./mvnw spring-boot:run
```

The app starts on **port 8081** and uses an in-memory H2 database seeded from `data.sql` on each run.

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI spec: `http://localhost:8081/api-docs`
- H2 console: `http://localhost:8081/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`)
- Actuator: `http://localhost:8081/actuator`

## Bank Dependency

`BankClient` is hardcoded to call a bank/acquirer service at `http://localhost:8787/api/v1`, expecting endpoints for `/authorizations`, `/captures`, `/voids`, and `/refunds`. This service must be running separately (e.g. a mock bank simulator) for authorize/capture/void/refund flows to succeed end-to-end.

## API Overview

All endpoints are under `/api/v1`.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/authorize` | Authorize a card payment (`amount`, `orderId`, `customerId`, `cardNumber`, `cvv`, `expiryMonth`, `expiryYear`) |
| GET | `/authorize/authorizationId=...` | Fetch authorization status from the bank |
| POST | `/capture?paymentRef=...` | Capture a previously authorized payment |
| GET | (capture lookup) `capture/{captureId}`=...` | Fetch capture status from the bank |
| POST | `/void?paymentRef=...` | Void an authorized payment |
| POST | `/refund?paymentRef=...` | Refund a captured payment |
| GET | (refund lookup) `?paymentRef=...` | Fetch refund status from the bank |
| POST | `/receipts` | Generate a receipt (`orderId`, `customerId`, `amount`) |
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
```

## Status

This is a project exploring payment gateway architecture: state machines, idempotency, and integration with an external bank API — treat it as a reference implementation rather than a deployable payment system
