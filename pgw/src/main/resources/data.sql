CREATE TABLE IF NOT EXISTS payments (
    paymentRef     VARCHAR PRIMARY KEY,
    authorizationId VARCHAR,   -- auth_<uuid> from the bank
    captureId       VARCHAR,
    voidId          VARCHAR,
    refundId        VARCHAR,
    orderId        VARCHAR,
    customerId     VARCHAR,
    amount          INTEGER,
    currentState    VARCHAR,    -- AUTHORIZED, CAPTURED, VOIDED, REFUNDED
    createdAt      TIMESTAMP,
    capturedAt      TIMESTAMP,
    voidedAt        TIMESTAMP,
    refundedAt      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS receipts (
    receiptId       VARCHAR PRIMARY KEY,
    paymentRef      VARCHAR,
    dateCreated     TIMESTAMP
);