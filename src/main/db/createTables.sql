
CREATE TABLE image (
  id BIGSERIAL PRIMARY KEY,
  size TEXT,
  url TEXT
);

CREATE TABLE imagemeta(
  id BIGSERIAL PRIMARY KEY,
  title TEXT,
  license TEXT,
  origin TEXT,
  small_id BIGINT REFERENCES image(id),
  full_id BIGINT REFERENCES image(id),
  external_id TEXT
);

CREATE TABLE imagetag(
  id BIGSERIAL PRIMARY KEY,
  tag TEXT,
  imagemeta_id BIGINT REFERENCES imagemeta(id)
);

CREATE TABLE imageauthor(
  id BIGSERIAL PRIMARY KEY,
  type TEXT,
  name TEXT,
  imagemeta_id BIGINT REFERENCES imagemeta(id)
);


