CREATE TABLE tilbakekreving_krav_statusmelding(
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_id INT NOT NULL REFERENCES tilbakekreving(id),
    vedtak_id BIGINT NOT NULL,
    kravstatuskode VARCHAR(128) NOT NULL,
    vedtak_gjelder_type VARCHAR(128) NOT NULL,
    vedtak_gjelder_ident VARCHAR(32) NOT NULL,
    referanse VARCHAR(64),
    kode_fagomraade VARCHAR(16) NOT NULL,
    fagsystem_id VARCHAR(64) NOT NULL,
    opprettet TIMESTAMP NOT NULL
);

CREATE INDEX ON tilbakekreving_krav_statusmelding(tilbakekreving_id);
