/*CREATE TABLE IF NOT EXISTS tilbakekreving_dataset.bq_behandling
(
    behandling_id STRING,
    opprettet_tid DATETIME,
    ytelses_type STRING,
    behandlende_enhet STRING,
    behandlingstype STRING,
)*/

DROP TABLE IF EXISTS `tilbakekreving_dataset.bq_behandling`;

CREATE TABLE `tilbakekreving_dataset.bq_behandling`
(
    tid TIMESTAMP,
    behandling_id STRING,
    opprettet_tid DATETIME,
    periode_fom DATE,
    periode_tom DATE,
    behandlingstype STRING,
    ytelses_type STRING,
    belop INT64,
    behandlende_enhet_navn STRING,
    behandlende_enhet_kode STRING,
    status STRING,
    resultat STRING
);
