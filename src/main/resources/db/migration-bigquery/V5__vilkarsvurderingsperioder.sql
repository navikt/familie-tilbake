CREATE TABLE `tilbakekreving_dataset.bq_vilkarsvurderingsperiode`
(
    tid TIMESTAMP,
    behandling_id STRING,
    periode_id STRING,
    periode_fom DATE,
    periode_tom DATE,
    ytelses_type STRING,
    rettslig_grunnlag STRING
);
