ALTER TABLE tilbakekreving_vilkårsvurdering_periode
    ADD COLUMN original_periode_id UUID,
    ADD COLUMN forrige_periode_id UUID;
