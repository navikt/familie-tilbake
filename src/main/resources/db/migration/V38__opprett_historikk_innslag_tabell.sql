CREATE TABLE historikkinnslag
(
    id                UUID PRIMARY KEY,
    behandling_id     UUID                                NOT NULL references behandling,
    type              VARCHAR                             NOT NULL,
    aktor             VARCHAR                             NOT NULL,
    tittel            VARCHAR                             NOT NULL,
    tekst             VARCHAR,
    steg              VARCHAR,
    journalpost_id    VARCHAR,
    dokument_id       VARCHAR,
    opprettet_av      VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT localtimestamp NOT NULL
);

CREATE INDEX ON historikkinnslag (behandling_id);

COMMENT ON TABLE historikkinnslag
    IS 'historikkinnslag over hendelser i saken';

COMMENT ON COLUMN historikkinnslag.id
    IS 'Primary key';

COMMENT ON COLUMN historikkinnslag.behandling_id
    IS 'Fagsystems/Tilbakekrevings behandling id som historikkinnslag er lagret for';

COMMENT ON COLUMN historikkinnslag.type
    IS 'Type av historikkinnslag';

COMMENT ON COLUMN historikkinnslag.aktor
    IS 'aktor type som opprettet historikkinnslaget';

COMMENT ON COLUMN historikkinnslag.tittel
    IS 'Tittel som vises i historikkinnslagfanen';

COMMENT ON COLUMN historikkinnslag.tekst
    IS 'Tekst som beskriver hendelsen (som skal vises i historikkinnslagfanen)';

COMMENT ON COLUMN historikkinnslag.steg
    IS 'vurderte stegnavn som opprettet historikkinnslaget';

COMMENT ON COLUMN historikkinnslag.journalpost_id
    IS 'Journalpost Id';

COMMENT ON COLUMN historikkinnslag.dokument_id
    IS 'Dokument Id';