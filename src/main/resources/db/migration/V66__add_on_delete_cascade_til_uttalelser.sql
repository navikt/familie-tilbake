ALTER TABLE tilbakekreving_uttalelse_informasjon
DROP CONSTRAINT tilbakekreving_uttalelse_informasjon_brukeruttalelse_ref_fkey;

ALTER TABLE tilbakekreving_uttalelse_informasjon
    ADD CONSTRAINT tilbakekreving_uttalelse_informasjon_brukeruttalelse_ref_fkey
        FOREIGN KEY (brukeruttalelse_ref)
            REFERENCES tilbakekreving_brukeruttalelse(id)
            ON DELETE CASCADE;
