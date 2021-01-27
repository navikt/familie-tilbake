ALTER TABLE fagsak
    DROP COLUMN version;
ALTER TABLE fagsak
    ADD COLUMN fagsystem varchar;
COMMENT ON COLUMN fagsak.fagsystem
    IS 'Fagsystemet som er eier av tilbakekrevingsbehandling';
ALTER TABLE behandling
    RENAME COLUMN ekstern_id TO ekstern_bruk_id;
ALTER TABLE behandling
    DROP COLUMN version;

ALTER TABLE ekstern_behandling
    DROP COLUMN version;
ALTER TABLE ekstern_behandling
    DROP COLUMN ekstern_id;
ALTER TABLE ekstern_behandling
    RENAME COLUMN henvisning TO ekstern_id;
ALTER TABLE ekstern_behandling
    ADD COLUMN versjon INTEGER DEFAULT 0  NOT NULL;

ALTER TABLE varsel
    DROP COLUMN version;
ALTER TABLE varsel
    ADD COLUMN versjon INTEGER DEFAULT 0  NOT NULL;
ALTER TABLE varsel
    ADD COLUMN revurderingsvedtaksdato DATE;
COMMENT ON COLUMN varsel.revurderingsvedtaksdato
    IS 'vedtaksdato av fagsystemsrevurdering. Brukes av varselbrev';

ALTER TABLE verge
    DROP COLUMN version;
ALTER TABLE verge
    ADD COLUMN versjon INTEGER DEFAULT 0  NOT NULL;
ALTER TABLE behandlingsresultat
    DROP COLUMN version;






