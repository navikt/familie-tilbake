ALTER TABLE tilbakekreving_vilkårsvurdering_periode_god_tro
    ADD COLUMN prosent_reduksjon INT,
    ADD COLUMN annet_begrunnelse TEXT,
    ADD COLUMN relevante_momenter VARCHAR(128)[];


