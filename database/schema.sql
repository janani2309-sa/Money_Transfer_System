-- Drop tables if they exist to allow clean resets
DROP TABLE IF EXISTS transaction_logs;
DROP TABLE IF EXISTS accounts;

-- Create ACCOUNTS table
CREATE TABLE accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    holder_name VARCHAR(255) NOT NULL,
    balance DECIMAL(18, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version INT DEFAULT 0 NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create TRANSACTION_LOGS table
CREATE TABLE transaction_logs (
    id VARCHAR(36) PRIMARY KEY,
    from_account BIGINT NOT NULL,
    to_account BIGINT NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255) NULL,
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_account) REFERENCES accounts(id),
    FOREIGN KEY (to_account) REFERENCES accounts(id)
);
