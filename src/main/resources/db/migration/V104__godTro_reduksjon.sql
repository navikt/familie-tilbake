ALTER TABLE tilbakekreving_vilkårsvurdering_periode_særlige_grunner ADD COLUMN
    relevante_momenter VARCHAR(128)[];

ALTER TABLE tilbakekreving_vilkårsvurdering_periode_særlige_grunner RENAME COLUMN
    annet_særlig_grunn_begrunnelse TO annet_begrunnelse;

