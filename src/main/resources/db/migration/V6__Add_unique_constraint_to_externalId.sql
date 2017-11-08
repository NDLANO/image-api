ALTER TABLE imagemetadata
ADD CONSTRAINT cst_uni_external_id UNIQUE (external_id);
