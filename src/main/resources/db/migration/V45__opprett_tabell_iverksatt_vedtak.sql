CREATE TABLE iverksatt_vedtak (
                                  id UUID NOT NULL PRIMARY KEY,
                                  behandling_id UUID NOT NULL,
                                  vedtak_id BIGINT,
                                  aktør JSONB NOT NULL,
                                  opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
                                  ytelsestype VARCHAR NOT NULL,
                                  kvittering VARCHAR NOT NULL,
                                  tilbakekrevingsperioder JSONB NOT NULL,
                                  behandlingstype VARCHAR NOT NULL
);
