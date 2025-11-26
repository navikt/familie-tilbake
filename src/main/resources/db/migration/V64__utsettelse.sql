ALTER TABLE tilbakekreving_utsett_uttalelse
    DROP COLUMN brukeruttalelse_ref,
    ADD COLUMN behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id);
