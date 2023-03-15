CREATE TABLE IF NOT EXISTS manuell_brevmottaker
(
    id             UUID PRIMARY KEY,
    behandling_id  UUID                                NOT NULL REFERENCES behandling,
    type           VARCHAR(50)                         NOT NULL,
    vergetype      VARCHAR(50),
    navn           VARCHAR                             NOT NULL,
    ident          VARCHAR,
    org_nr         VARCHAR,
    adresselinje_1 VARCHAR,
    adresselinje_2 VARCHAR,
    postnummer     VARCHAR,
    poststed       VARCHAR,
    landkode       VARCHAR(2),
    versjon        BIGINT       DEFAULT 0               NOT NULL,
    opprettet_av   VARCHAR      DEFAULT 'VL'            NOT NULL,
    opprettet_tid  TIMESTAMP(3) DEFAULT localtimestamp  NOT NULL,
    endret_av      VARCHAR,
    endret_tid     TIMESTAMP(3)
);

CREATE INDEX IF NOT EXISTS manuell_brevmottaker_behandling_id_idx ON manuell_brevmottaker (behandling_id);
