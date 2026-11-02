-- Wallet Service — initial schema
-- Flyway applies this once and records it in flyway_schema_history.
-- Never edit this file after deployment; add a V2__ file for changes instead.

CREATE TABLE IF NOT EXISTS wallets (
    id          VARCHAR(36)     NOT NULL,
    owner_name  VARCHAR(255)    NOT NULL,
    balance     DECIMAL(19, 4)  NOT NULL,
    currency    VARCHAR(10)     NOT NULL,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    id             VARCHAR(36)     NOT NULL,
    wallet_id      VARCHAR(36)     NOT NULL,
    type           VARCHAR(20)     NOT NULL,
    amount         DECIMAL(19, 4)  NOT NULL,
    balance_before DECIMAL(19, 4),
    balance_after  DECIMAL(19, 4),
    description    VARCHAR(500),
    reference_id   VARCHAR(36),
    occurred_at    DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_txn_wallet_id (wallet_id),
    INDEX idx_txn_occurred_at (occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
