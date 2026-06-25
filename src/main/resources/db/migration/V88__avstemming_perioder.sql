CREATE TABLE iverksatt_vedtak_periode
(
    id                   UUID NOT NULL PRIMARY KEY,
    iverksatt_vedtak_ref UUID NOT NULL REFERENCES iverksatt_vedtak(id),
    fom                  DATE NOT NULL,
    tom                  DATE NOT NULL,
    beløp_tilbakekreves  TEXT NOT NULL,
    skattebeløp          TEXT NOT NULL,
    rentebeløp           TEXT NOT NULL
);

INSERT INTO iverksatt_vedtak_periode (id, iverksatt_vedtak_ref, fom, tom, beløp_tilbakekreves, skattebeløp, rentebeløp)
SELECT gen_random_uuid(),
       iv.id,
       (periode -> 'periode' ->> 'fom')::date,
       (periode -> 'periode' ->> 'tom')::date,
       (SELECT SUM((belop ->> 'belopTilbakekreves')::numeric)
        FROM jsonb_array_elements(periode -> 'tilbakekrevingsbelop') AS belop)::text,
       (SELECT SUM((belop ->> 'belopSkatt')::numeric)
        FROM jsonb_array_elements(periode -> 'tilbakekrevingsbelop') AS belop)::text,
       periode ->> 'belopRenter'
FROM iverksatt_vedtak iv,
     jsonb_array_elements(tilbakekrevingsvedtak -> 'tilbakekrevingsperiode') AS periode;

ALTER TABLE iverksatt_vedtak
    RENAME COLUMN opprettet_tid TO vedtaksdato;
ALTER TABLE iverksatt_vedtak
    ALTER COLUMN vedtaksdato TYPE DATE USING vedtaksdato::date;
ALTER TABLE iverksatt_vedtak
    ADD COLUMN ny_modell BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE iverksatt_vedtak
    ALTER COLUMN ny_modell DROP DEFAULT;
ALTER TABLE iverksatt_vedtak
    DROP COLUMN tilbakekrevingsvedtak,
    DROP COLUMN opprettet_av,
    DROP COLUMN endret_av,
    DROP COLUMN endret_tid;
