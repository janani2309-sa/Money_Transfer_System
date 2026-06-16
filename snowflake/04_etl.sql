-- Snowflake ETL Pipeline Script

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA ANALYTICS;

-- 1. Create Staging Tables for Raw Data
CREATE OR REPLACE TEMPORARY TABLE STG_ACCOUNTS (
    id NUMBER,
    holder_name VARCHAR(255),
    balance DECIMAL(18,2),
    status VARCHAR(20),
    version NUMBER,
    last_updated TIMESTAMP
);

CREATE OR REPLACE TEMPORARY TABLE STG_TRANSACTION_LOGS (
    id VARCHAR(36),
    from_account NUMBER,
    to_account NUMBER,
    amount DECIMAL(18,2),
    status VARCHAR(20),
    failure_reason VARCHAR(255),
    idempotency_key VARCHAR(100),
    created_on TIMESTAMP
);

-- 2. COPY INTO Commands (Copies raw CSV data from Stage to Staging Tables)
-- Note: File names are matched to target CSV files uploaded to the stage
COPY INTO STG_ACCOUNTS
  FROM @transactions_stage/accounts.csv
  FILE_FORMAT = csv_file_format
  ON_ERROR = 'CONTINUE';

COPY INTO STG_TRANSACTION_LOGS
  FROM @transactions_stage/transaction_logs.csv
  FILE_FORMAT = csv_file_format
  ON_ERROR = 'CONTINUE';

-- 3. Populate DIM_ACCOUNT from Staging Data
-- Insert new accounts or updates (simple insert for progressive build demo)
INSERT INTO DIM_ACCOUNT (account_id, holder_name, status, effective_date)
SELECT 
    id,
    holder_name,
    status,
    last_updated::DATE
FROM STG_ACCOUNTS
WHERE id NOT IN (SELECT account_id FROM DIM_ACCOUNT);

-- 4. Populate FACT_TRANSACTIONS
-- Resolves business IDs to Dimension Surrogate Keys and dates to DIM_DATE keys
INSERT INTO FACT_TRANSACTIONS (account_from_key, account_to_key, date_key, amount, status)
SELECT 
    da_from.account_key as account_from_key,
    da_to.account_key as account_to_key,
    REPLACE(TO_VARCHAR(stg.created_on::DATE), '-', '')::NUMBER as date_key,
    stg.amount,
    stg.status
FROM STG_TRANSACTION_LOGS stg
JOIN DIM_ACCOUNT da_from ON stg.from_account = da_from.account_id
JOIN DIM_ACCOUNT da_to ON stg.to_account = da_to.account_id;
