-- Elg
-- TODO: Bør ikke inserte testdata på denne måten. Vet ikke hva id-er blir.
insert into image(size, url) values ('8680', 'http://api.test.ndla.no/images/thumbs/Elg.jpg');
insert into image(size, url) values ('2865539', 'http://api.test.ndla.no/images/full/Elg.jpg');
insert into imagemeta(title, license, origin, small_id, full_id, external_id) values ('Elg i busk', 'by-nc-sa', 'http://www.scanpix.no', 1, 2, '123');
insert into imagetag(tag, imagemeta_id) values ('rovdyr', 1);
insert into imagetag(tag, imagemeta_id) values ('elg', 1);
insert into imagetag(tag, imagemeta_id) values ('busk', 1);
insert into imageauthor(type, name, imagemeta_id) values ('Fotograf', 'Test Testesen', 1);

-- Rein
insert into image(size, url) values ('8680', 'http://api.test.ndla.no/images/thumbs/Rein.jpg');
insert into image(size, url) values ('2865539', 'http://api.test.ndla.no/images/full/Rein.jpg');
insert into imagemeta(title, license, origin, small_id, full_id, external_id) values ('Rein har fanget type', 'by-nc-sa', 'http://www.scanpix.no', 3, 4, '123');
insert into imagetag(tag, imagemeta_id) values ('rovdyr', 2);
insert into imagetag(tag, imagemeta_id) values ('rein', 2);
insert into imageauthor(type, name, imagemeta_id) values ('Fotograf', 'Rolf Rolfsen', 2);
insert into imageauthor(type, name, imagemeta_id) values ('Leverandør', 'Scanpix', 2);