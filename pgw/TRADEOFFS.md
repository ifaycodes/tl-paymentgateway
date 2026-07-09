# Tradeoffs & Design Decisions

## Architecture
The gateway is structured in three layers:

- **GatewayController** — exposes REST endpoints to FicMart and orchestrates the flow
- **BankClient** — handles all HTTP communication with the Mock Bank API
- **PaymentRepository** — stores core payment data (paymentRef, orderId, customerId, amount, currency, currentState, createdAt) — no bank transaction IDs here on purpose, see below
- **PaymentEventRepository** — stores a log of every state change per payment
- **AuditRepository** — stores a log of every bank reseponse, whether successful or not, ffrom the bank client before any mapping

I split payments and payment events into two tables. 
A single payments table would accumulate nullable columns (captureId, voidId, refundId, capturedAt, voidedAt, refundedAt) that only get populated at specific stages. 
Instead, every operation writes an event row in payment event table with the paymentRef, bankId, timestamp, note, and new state — that way, the payments table is clean and giving a full audit trail of every transition.

## State Management
Payment state is tracked using a `State` enum (PENDING, APPROVED, CAPTURED, VOIDED, REFUNDED, FAILED) with direct database updates on each operation. 
This keeps state management simple and explicit — the DB is the single source of truth (I don't like how this sounds). 
Every state change is also written to the PaymentEventRepository, so the full history of a payment is always available even if the current state is all that's needed for most queries.

## Failure Handling
The gateway implements a retry strategy for bank calls that fail with transient errors — 5xx responses, and connection-level failures (`ConnectException`, closed channels, etc.), which used to propagate as raw uncaught exceptions instead of classifying as transient. Both cases now throw `BankNotConnectingException` and go through the same retry path.
Each operation retries up to 3 times with an exponential-style backoff — sleeping `1000 * attempt` milliseconds between retries (1s, 2s, 3s). If all 3 retries fail, then FicMart receives an error response.

Partial failures — for example, the bank confirms a capture but the DB write fails — are not yet fully handled. 
In that scenario the payment state in the DB would not reflect the actual bank state - check [idempotency](#idempotency)

## Idempotency
At the bank client level, idempotency keys are derived from the parameters I am using to call the endpoint using UUID, ensuring that retries of the same payment attempt always send the same key. 
This means the bank will never double-charge even if the gateway retries after a transient failure.
A known edge case: if the gateway crashes after calling the bank but before saving the result, on recovery the gateway would call the bank again with the same idempotency key and get the original response back — making the retry safe. 
This is the primary reason for saving payments as PENDING before calling the bank.
Before calling the bank for any operation, the gateway checks whether the incoming idempotency key has already been processed. This is a boolean lookup in the DB — if the key exists, I set it up that the gateway doesn't call the bank at all.. 
Also having the DB write at the end makes it that if we have a partial failure — bank returns but db doesn't write — that way the initial boolean check returns false, so the bank gets called again which it will return a cached response that will then get written up. Also audit log helps keep track of responses from the bank

This covers all four operations: authorize, capture, void, and refund, each generating their own idempotency key.

-- I did consider using the same idem key for the same transaction across all operations but decided to go with this to keep things cleaner. The primary key remains payment ref

## What I'd Do Differently
- **Idempotency keys** — what I use to and how I generate the idem key I have found is prone to a lot of errors that could go wrong the client side
- **Partial failure handling** — implement a reconciliation job that checks bank state against DB state and corrects mismatches
- **Real database** — I would replace H2 with PostgreSQL for production, H2 is in-memory only and loses all data on restart
- **State changes** — I am currently manually updating the state of each operation. I would have a more robust way to move this update. I have a `StateMachine` class set up but it's currently not wired in.
- **Cast a wider error net** — malformed/unexpected bank response bodies, and request timeouts aren't distinguished from other connection failures


### For the testing
I tried to manually bootstrap the tests, setting the expected responses or telling mock bank client what to return. But i figured that doesn't actually test the code, since I'm telling it what to say. 
So I used SpringBoot Test, which I think is not doing a good job. The tests return well but I don't actually get to really rough it up since i already know what it would return

The concrete problem this causes: `BankTest` makes real HTTP calls to a live bank service, which makes it an integration test instead of a unit test. It can't run anywhere without network access to a bank — which is exactly why it breaks when I run `mvn clean package` inside the Docker build stage (isolated, no route to any bank), forcing tests to be skipped there (`-DskipTests`) rather than actually run in CI. Stubbing the HTTP layer (WireMock or similar) so `BankTest` becomes a real unit test that doesn't need a live bank at all, I found would help. Haven't implemented it.