-- Schema
CREATE SCHEMA imageapi;

-- READONLY
CREATE USER imageapi_read with PASSWORD '<passord>';
ALTER DEFAULT PRIVILEGES IN SCHEMA imageapi GRANT SELECT ON TABLES TO imageapi_read;

GRANT CONNECT ON DATABASE data_test to imageapi_read;
GRANT USAGE ON SCHEMA imageapi to imageapi_read;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA imageapi TO imageapi_read;
GRANT SELECT ON ALL TABLES IN SCHEMA imageapi TO imageapi_read;

-- WRITE
CREATE USER imageapi_write with PASSWORD '<passord>';

GRANT CONNECT ON DATABASE data_test to imageapi_write;
GRANT USAGE ON SCHEMA imageapi to imageapi_write;
GRANT CREATE ON SCHEMA imageapi to imageapi_write;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA imageapi TO imageapi_write;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA imageapi TO imageapi_write;