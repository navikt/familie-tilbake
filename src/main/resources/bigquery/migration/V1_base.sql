CREATE TABLE IF NOT EXISTS tilbakekreving_dataset.bq_behandling
(
    behandling_id STRING,
    opprettet_tid DATETIME,
    behandlingstype STRING,
    ytelses_type STRING,
    under_ytelse STRING,
    belop NUMERIC,
    behandlende_enhet STRING,
    behandlingsstatus STRING,
    vedtaksresultat STRING
);
