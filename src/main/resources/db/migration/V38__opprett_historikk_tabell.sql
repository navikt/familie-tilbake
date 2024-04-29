CREATE TABLE historikk
(
    id                UUID PRIMARY KEY,
    behandling_id     UUID                                NOT NULL references behandling,
    ekstern_fagsak_id VARCHAR                             NOT NULL,
    fagsystem         VARCHAR                             NOT NULL,
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

COMMENT ON TABLE historikk
    IS 'Historikk over hendelser i saken';

COMMENT ON COLUMN historikk.id
    IS 'Primary key';

COMMENT ON COLUMN historikk.behandling_id
    IS 'Fagsystems/Tilbakekrevings behandling id som historikk er lagret for';

COMMENT ON COLUMN historikk.ekstern_fagsak_id
    IS 'Saksnummer (som gsak har mottatt)';

COMMENT ON COLUMN historikk.fagsystem
    IS 'Fagsystemet som historikk er lagret for';

COMMENT ON COLUMN historikk.type
    IS 'Type av historikk';

COMMENT ON COLUMN historikk.aktor
    IS 'aktor type som opprettet historikket';

COMMENT ON COLUMN historikk.tittel
    IS 'Tittel som vises i historikkfanen';

COMMENT ON COLUMN historikk.tekst
    IS 'Tekst som beskriver hendelsen (som skal vises i historikkfanen)';

COMMENT ON COLUMN historikk.steg
    IS 'vurderte stegnavn som opprettet historikket';

COMMENT ON COLUMN historikk.journalpost_id
    IS 'Journalpost Id';

COMMENT ON COLUMN historikk.dokument_id
    IS 'Dokument Id';