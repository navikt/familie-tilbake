CREATE TABLE ekstern_behandling (
    id            UUID PRIMARY KEY,
    version       BIGINT                              NOT NULL,
    behandling_id UUID                                NOT NULL REFERENCES behandling,
    aktiv         BOOLEAN      DEFAULT TRUE           NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    henvisning    VARCHAR                             NOT NULL,
    UNIQUE (behandling_id, henvisning)
);

COMMENT ON TABLE ekstern_behandling
    IS 'Referanse til ekstern behandling';

COMMENT ON COLUMN ekstern_behandling.id
    IS 'Primary key';

COMMENT ON COLUMN ekstern_behandling.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til intern behandling';

COMMENT ON COLUMN ekstern_behandling.aktiv
    IS 'Angir om ekstern behandling data er gjeldende';

COMMENT ON COLUMN ekstern_behandling.henvisning
    IS 'Henvisning;referanse. Peker på referanse-feltet i kravgrunnlaget, og kommer opprinnelig fra fagsystemet.';

CREATE INDEX ON ekstern_behandling (behandling_id);

CREATE INDEX ON ekstern_behandling (henvisning);
