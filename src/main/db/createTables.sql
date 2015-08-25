
CREATE TABLE image (
  id BIGSERIAL PRIMARY KEY,
  url TEXT,
  size INTEGER,
  content_type TEXT
);

CREATE TABLE imagemeta(
  id BIGSERIAL PRIMARY KEY,
  license TEXT,
  origin TEXT,
  small_id BIGINT REFERENCES image(id),
  full_id BIGINT REFERENCES image(id),
  external_id TEXT
);

CREATE TABLE imagetag(
  id BIGSERIAL PRIMARY KEY,
  tag TEXT,
  language TEXT,
  imagemeta_id BIGINT REFERENCES imagemeta(id)
);

CREATE TABLE imageauthor(
  id BIGSERIAL PRIMARY KEY,
  type TEXT,
  name TEXT,
  imagemeta_id BIGINT REFERENCES imagemeta(id)
);

CREATE TABLE imagetitle(
  id BIGSERIAL PRIMARY KEY,
  title TEXT,
  language TEXT,
  imagemeta_id BIGINT REFERENCES imagemeta(id)
);

