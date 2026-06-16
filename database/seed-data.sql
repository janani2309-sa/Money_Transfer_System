-- Seed Accounts
-- Account 1 matching page 15 wireframe: John Smith, balance 45250.00
INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (1, 'John Smith', 45250.00, 'ACTIVE', 0);

INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (2, 'Jane Doe', 10000.00, 'ACTIVE', 0);

INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (3, 'Bob Wilson', 15000.00, 'ACTIVE', 0);

INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (4, 'Alice Brown', 750.00, 'ACTIVE', 0);

INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (5, 'Mike Chen', 2500.00, 'ACTIVE', 0);

INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (6, 'Locked User', 5000.00, 'LOCKED', 0);

INSERT INTO accounts (id, holder_name, balance, status, version) 
VALUES (7, 'Closed User', 0.00, 'CLOSED', 0);
