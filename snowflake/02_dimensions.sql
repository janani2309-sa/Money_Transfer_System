-- Snowflake Dimensions Definition

USE DATABASE MONEY_TRANSFER_DW;
USE SCHEMA ANALYTICS;

-- Create Account Dimension Table
CREATE OR REPLACE TABLE DIM_ACCOUNT (
    account_key NUMBER IDENTITY(1,1) PRIMARY KEY,
    account_id NUMBER NOT NULL,
    holder_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    effective_date DATE DEFAULT CURRENT_DATE()
);

-- Create Date Dimension Table
CREATE OR REPLACE TABLE DIM_DATE (
    date_key NUMBER PRIMARY KEY, -- YYYYMMDD
    full_date DATE NOT NULL,
    day NUMBER NOT NULL,
    month NUMBER NOT NULL,
    year NUMBER NOT NULL,
    quarter NUMBER NOT NULL
);

-- Populate Date Dimension (Seeds a range of dates from 2025 to 2027)
INSERT INTO DIM_DATE (date_key, full_date, day, month, year, quarter)
SELECT 
    REPLACE(TO_VARCHAR(d), '-', '')::NUMBER as date_key,
    d as full_date,
    EXTRACT(DAY FROM d) as day,
    EXTRACT(MONTH FROM d) as month,
    EXTRACT(YEAR FROM d) as year,
    EXTRACT(QUARTER FROM d) as quarter
FROM (
    SELECT DATEADD(DAY, SEQ4(), '2025-01-01'::DATE) as d
    FROM TABLE(GENERATOR(ROWCOUNT => 1095)) -- 3 years
);
