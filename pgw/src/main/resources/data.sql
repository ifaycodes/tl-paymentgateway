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
    timeCreated     VARCHAR,
    notes           VARCHAR
);

CREATE TABLE IF NOT EXISTS logs (
    paymentRef      VARCHAR,
    response        VARCHAR,
    timeCreated     TIMESTAMP

);