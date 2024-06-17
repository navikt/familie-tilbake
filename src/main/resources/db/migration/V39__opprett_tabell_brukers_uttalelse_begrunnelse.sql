CREATE TABLE vurdering_av_brukers_uttalelse
(
    id                    UUID PRIMARY KEY,
    fakta_feilutbetaling  UUID                                NOT NULL references fakta_feilutbetaling,
    har_bruker_uttalt_seg VARCHAR                             NOT NULL,
    beskrivelse           VARCHAR,
    aktiv                 BOOLEAN      DEFAULT TRUE           NOT NULL,
    versjon               BIGINT                              NOT NULL,
    opprettet_av          VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid         TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av             VARCHAR                             NOT NULL,
    endret_tid            TIMESTAMP(3)                        NOT NULL
);

CREATE INDEX ON vurdering_av_brukers_uttalelse (fakta_feilutbetaling);