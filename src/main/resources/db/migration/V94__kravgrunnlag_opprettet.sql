ALTER TABLE tilbakekreving_kravgrunnlag
    ADD COLUMN opprettet TIMESTAMP;

UPDATE tilbakekreving_kravgrunnlag
SET opprettet = TO_TIMESTAMP(kontrollfelt, 'YYYY-MM-DD-HH24.MI.SS.US');

ALTER TABLE tilbakekreving_kravgrunnlag
    ALTER COLUMN opprettet SET NOT NULL;
