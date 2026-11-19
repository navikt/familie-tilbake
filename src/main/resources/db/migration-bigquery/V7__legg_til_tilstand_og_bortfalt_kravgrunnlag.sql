ALTER TABLE `tilbakekreving_dataset.bq_behandling`
    ADD COLUMN tilstand STRING,
    ADD COLUMN er_infobrev_sendt BOOL,
    ADD COLUMN er_kravgrunnlag_bortfalt BOOL;
