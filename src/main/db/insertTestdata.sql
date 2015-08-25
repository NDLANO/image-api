-- Elg
-- TODO: Bør ikke inserte testdata på denne måten. Vet ikke hva id-er blir.
insert into image(size, url, content_type) values ('8680', 'http://api.test.ndla.no/images/thumbs/Elg.jpg', 'image/jpeg');
insert into image(size, url, content_type) values ('2865539', 'http://api.test.ndla.no/images/full/Elg.jpg', 'image/jpeg');
insert into imagemeta(license, origin, small_id, full_id, external_id) values ('by-nc-sa', 'http://www.scanpix.no', 1, 2, '123');
insert into imagetitle(title, language, imagemeta_id) values ('Elg i busk', 'nb', 1);
insert into imagetitle(title, language, imagemeta_id) values ('Elk in bush', 'en', 1);
insert into imagetag(tag, language, imagemeta_id) values ('rovdyr', 'nb', 1);
insert into imagetag(tag, language, imagemeta_id) values ('elg', 'nb', 1);
insert into imagetag(tag, language, imagemeta_id) values ('busk', 'nb', 1);
insert into imagetag(tag, language, imagemeta_id) values ('predator', 'en', 1);
insert into imagetag(tag, language, imagemeta_id) values ('elk', 'en', 1);
insert into imagetag(tag, language, imagemeta_id) values ('bush', 'en', 1);
insert into imageauthor(type, name, imagemeta_id) values ('Fotograf', 'Test Testesen', 1);

-- Rein
insert into image(size, url, content_type) values ('8680', 'http://api.test.ndla.no/images/thumbs/Rein.jpg', 'image/jpeg');
insert into image(size, url, content_type) values ('2865539', 'http://api.test.ndla.no/images/full/Rein.jpg', 'image/jpeg');
insert into imagemeta(license, origin, small_id, full_id, external_id) values ('by-nc-sa', 'http://www.scanpix.no', 3, 4, '123');
insert into imagetitle(title, language, imagemeta_id) values ('Rein har fanget type', 'nb', 2);
insert into imagetitle(title, language, imagemeta_id) values ('Reindeer has caught some guy', 'en', 2);
insert into imagetag(tag, language, imagemeta_id) values ('rovdyr','nb', 2);
insert into imagetag(tag, language, imagemeta_id) values ('rein', 'nb', 2);
insert into imagetag(tag, language, imagemeta_id) values ('predator','en', 2);
insert into imagetag(tag, language, imagemeta_id) values ('reindeer', 'en', 2);
insert into imageauthor(type, name, imagemeta_id) values ('Fotograf', 'Rolf Rolfsen', 2);
insert into imageauthor(type, name, imagemeta_id) values ('Leverandør', 'Scanpix', 2);