ALTER TABLE ekstern_behandling
    RENAME TO fagsystemsbehandling;

ALTER TABLE fagsystemsbehandling
    ADD COLUMN tilbakekrevingsvalg VARCHAR;

ALTER TABLE fagsystemsbehandling
    ADD COLUMN resultat VARCHAR;

CREATE TABLE fagsystemsbehandlingsarsak (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    arsak                   VARCHAR                             NOT NULL,
    fagsystemsbehandling_id UUID                                NOT NULL REFERENCES fagsystemsbehandling,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE fagsystemsbehandlingsarsak
    IS 'Fagsystems revurderingsårsaker, vises i Fakta';

COMMENT ON COLUMN fagsystemsbehandlingsarsak.id
    IS 'Primary key';

COMMENT ON COLUMN fagsystemsbehandlingsarsak.arsak
    IS 'Årsaken til fagsystemsrevurdering';

COMMENT ON COLUMN fagsystemsbehandlingsarsak.fagsystemsbehandling_id
    IS 'FK:fagsystemsbehandling fremmednøkkel';

CREATE INDEX ON fagsystemsbehandlingsarsak (fagsystemsbehandling_id);

CREATE TABLE fagsystemskonsekvens (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    konsekvens              VARCHAR                             NOT NULL,
    fagsystemsbehandling_id UUID                                NOT NULL REFERENCES fagsystemsbehandling,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE fagsystemskonsekvens
    IS 'Fagsystemskonsekvens for ytelser, vises i Fakta';

COMMENT ON COLUMN fagsystemskonsekvens.id
    IS 'Primary key';

COMMENT ON COLUMN fagsystemskonsekvens.konsekvens
    IS 'Konsekvens for ytelser til fagsystemsrevurdering';

COMMENT ON COLUMN fagsystemskonsekvens.fagsystemsbehandling_id
    IS 'FK:fagsystemsbehandling fremmednøkkel';

CREATE INDEX ON fagsystemskonsekvens (fagsystemsbehandling_id);

