-- db instance identifier: ndla-image-api

-- master user: imageapi_master
-- master password: spgjdxnZqYV2jv6

-- database name: ndla_image_api_test
-- database port: 5432

-- read user: imageapi_read
-- read password: Qwf9m7PywTJTrti

-- write user: imageapi_write
-- write password: cx8QnLj9qEszrep


-- READONLY
CREATE USER imageapi_read with PASSWORD '<passord>';
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO imageapi_read;

GRANT CONNECT ON DATABASE imagemeta_prod to imageapi_read;
GRANT USAGE ON SCHEMA public to imageapi_read;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO imageapi_read;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO imageapi_read;

-- WRITE
CREATE USER imageapi_write with PASSWORD '<passord>';

GRANT CONNECT ON DATABASE imagemeta_prod to imageapi_write;
GRANT USAGE ON SCHEMA public to imageapi_write;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO imageapi_write;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO imageapi_write;