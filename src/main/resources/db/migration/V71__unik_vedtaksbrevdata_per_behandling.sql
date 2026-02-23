ALTER TABLE tilbakekreving_behandling_vedtaksbrev
    ADD CONSTRAINT behandling_ref_unique UNIQUE (behandling_ref);
