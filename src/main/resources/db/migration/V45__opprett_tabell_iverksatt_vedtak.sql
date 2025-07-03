CREATE TABLE iverksatt_vedtak (
    id UUID NOT NULL PRIMARY KEY,
    behandling_id UUID NOT NULL,
    vedtak_id BIGINT,
    aktør JSONB NOT NULL,
    ytelsestype VARCHAR NOT NULL,
    kvittering VARCHAR NOT NULL,
    tilbakekrevingsvedtak JSONB NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    behandlingstype VARCHAR NOT NULL
);
