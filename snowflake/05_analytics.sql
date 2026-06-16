-- Snowflake Business Intelligence Analytics Queries

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA ANALYTICS;

-- Query 1: Daily Transaction Volume (Count and sum of transactions by day)
SELECT 
    d.full_date as transaction_date,
    COUNT(f.transaction_key) as total_transaction_count,
    SUM(f.amount) as total_transaction_amount
FROM FACT_TRANSACTIONS f
JOIN DIM_DATE d ON f.date_key = d.date_key
GROUP BY d.full_date
ORDER BY d.full_date DESC;


-- Query 2: Account Activity (Identify most active accounts based on transactions sent/received)
SELECT 
    da.holder_name,
    da.account_id,
    COUNT(f.transaction_key) as total_activities,
    SUM(CASE WHEN f.account_from_key = da.account_key THEN f.amount ELSE 0 END) as total_sent,
    SUM(CASE WHEN f.account_to_key = da.account_key THEN f.amount ELSE 0 END) as total_received
FROM FACT_TRANSACTIONS f
JOIN DIM_ACCOUNT da ON f.account_from_key = da.account_key OR f.account_to_key = da.account_key
GROUP BY da.holder_name, da.account_id
ORDER BY total_activities DESC
LIMIT 10;


-- Query 3: Success Rate (Percentage of successful vs failed transactions)
SELECT 
    status,
    COUNT(transaction_key) as count,
    ROUND((COUNT(transaction_key) * 100.0) / SUM(COUNT(transaction_key)) OVER (), 2) as percentage
FROM FACT_TRANSACTIONS
GROUP BY status;


-- Query 4: Peak Hours (Busiest transfer times of day, extracted from staging logs timestamps)
-- Note: Uses the raw created_on timestamp from STG_TRANSACTION_LOGS to find hour of day
SELECT 
    EXTRACT(HOUR FROM created_on) as hour_of_day,
    COUNT(id) as transaction_count,
    SUM(amount) as total_volume
FROM STG_TRANSACTION_LOGS
GROUP BY hour_of_day
ORDER BY transaction_count DESC;


-- Query 5: Average Transfer Amount (Average value of all successful transactions)
SELECT 
    COUNT(transaction_key) as successful_transfers_count,
    ROUND(AVG(amount), 2) as average_transfer_amount,
    MIN(amount) as min_transfer_amount,
    MAX(amount) as max_transfer_amount
FROM FACT_TRANSACTIONS
WHERE status = 'SUCCESS';
