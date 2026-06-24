-- Seed Users
INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (1, 'janani', '$2a$10$dC4rm8w7qPJD34rmygL0puIdie7EU/2xY0w0os5fIu/00ORRbAzRi', 'John', 'Smith', 'janani@apexpay.com', '1234567890', TRUE);

INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (2, 'jane', '$2a$10$eyFbKZ95/SEDvwxM41sma.zErtsb0VWYtB5.mUzs8gaJEU2HyVurm', 'Jane', 'Doe', 'jane@apexpay.com', '5551234567', TRUE);

INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (3, 'admin', '$2a$10$ZB3FEDauO5SbDkRM/8XR6eIf/mwGLIusuH/OQp3EGbdqODNC8Aike', 'Bob', 'Wilson', 'admin@apexpay.com', '9876543210', TRUE);

INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (4, 'alice', '$2a$10$eyFbKZ95/SEDvwxM41sma.zErtsb0VWYtB5.mUzs8gaJEU2HyVurm', 'Alice', 'Brown', 'alice@apexpay.com', '5557654321', TRUE);

INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (5, 'mike', '$2a$10$eyFbKZ95/SEDvwxM41sma.zErtsb0VWYtB5.mUzs8gaJEU2HyVurm', 'Mike', 'Chen', 'mike@apexpay.com', '5551112222', TRUE);

INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (6, 'locked', '$2a$10$eyFbKZ95/SEDvwxM41sma.zErtsb0VWYtB5.mUzs8gaJEU2HyVurm', 'Locked', 'User', 'locked@apexpay.com', '5552223333', TRUE);

INSERT INTO users (id, username, password, first_name, last_name, email, phone_number, is_verified)
VALUES (7, 'closed', '$2a$10$eyFbKZ95/SEDvwxM41sma.zErtsb0VWYtB5.mUzs8gaJEU2HyVurm', 'Closed', 'User', 'closed@apexpay.com', '5553334444', TRUE);

-- Seed Accounts with structured account numbers, first name, last name, and user links
-- Account 1 matching page 15 wireframe: John Smith, balance 45250.00
INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (1, 'APXAC00001', 1, 'SAVINGS', 'John', 'Smith', 45250.00, 'ACTIVE', 0);

INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (2, 'APXAC00002', 2, 'SAVINGS', 'Jane', 'Doe', 10000.00, 'ACTIVE', 0);

INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (3, 'APXAC00003', 3, 'SAVINGS', 'Bob', 'Wilson', 15000.00, 'ACTIVE', 0);

INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (4, 'APXAC00004', 4, 'SAVINGS', 'Alice', 'Brown', 2000.00, 'ACTIVE', 0);

INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (5, 'APXAC00005', 5, 'SAVINGS', 'Mike', 'Chen', 2500.00, 'ACTIVE', 0);

INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (6, 'APXAC00006', 6, 'SAVINGS', 'Locked', 'User', 5000.00, 'LOCKED', 0);

INSERT INTO accounts (id, account_number, user_id, account_type, first_name, last_name, balance, status, version) 
VALUES (7, 'APXAC00007', 7, 'SAVINGS', 'Closed', 'User', 1000.00, 'CLOSED', 0);
