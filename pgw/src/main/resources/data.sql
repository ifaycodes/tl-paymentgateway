CREATE TABLE IF NOT EXISTS payments (
    paymentRef     VARCHAR PRIMARY KEY,
    orderId        VARCHAR,
    customerId     VARCHAR,
    amount          INTEGER,
    currentState    VARCHAR,
    currency        VARCHAR,
    createdAt       VARCHAR

);

CREATE TABLE IF NOT EXISTS paymentevent (
    paymentRef      VARCHAR,
    idempotencyKey  VARCHAR,
    currentState    VARCHAR,
    bankTransactionId   VARCHAR,
    timestamp     TIMESTAMP,
    notes           VARCHAR
);

CREATE TABLE IF NOT EXISTS auditLog (

);