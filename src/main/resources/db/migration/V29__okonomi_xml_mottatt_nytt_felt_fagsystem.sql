ALTER TABLE okonomi_xml_mottatt
    ADD COLUMN fagsystem VARCHAR;

UPDATE okonomi_xml_mottatt
SET fagsystem = 'BA'
WHERE ytelsestype = 'BARNETRYGD';

UPDATE okonomi_xml_mottatt
SET fagsystem = 'EF'
WHERE ytelsestype = 'OVERGANGSSTØNAD';

UPDATE okonomi_xml_mottatt
SET fagsystem = 'EF'
WHERE ytelsestype = 'BARNETILSYN';

UPDATE okonomi_xml_mottatt
SET fagsystem = 'EF'
WHERE ytelsestype = 'SKOLEPENGER';

ALTER TABLE okonomi_xml_mottatt
    ALTER COLUMN fagsystem SET NOT NULL;
