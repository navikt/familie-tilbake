ALTER TABLE tilbakekreving_dataset.bq_behandling
    ALTER COLUMN periode_fom DROP NOT NULL;
ALTER TABLE tilbakekreving_dataset.bq_behandling
    ALTER COLUMN periode_tom DROP NOT NULL;
