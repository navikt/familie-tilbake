CREATE TABLE behandlingsresultat (
    id            UUID PRIMARY KEY,
    version       BIGINT                               NOT NULL,
    behandling_id UUID                                 NOT NULL REFERENCES behandling,
    type          VARCHAR      DEFAULT 'IKKE_FASTSATT' NOT NULL,
    versjon       INTEGER      DEFAULT 0               NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'            NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp  NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE behandlingsresultat
    IS 'Beregningsresultat. Knytter sammen beregning og behandling.';

COMMENT ON COLUMN behandlingsresultat.id
    IS 'Primary key';

COMMENT ON COLUMN behandlingsresultat.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til behandling';

COMMENT ON COLUMN behandlingsresultat.type
    IS 'Resultat av behandlingen';

CREATE INDEX ON behandlingsresultat (behandling_id);

CREATE INDEX ON behandlingsresultat (type);

