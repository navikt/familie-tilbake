ALTER TABLE tilbakekreving_vilkårsvurdering_periode_særlige_grunner RENAME COLUMN
    særlige_grunner TO reduksjon_momenter;

ALTER TABLE tilbakekreving_vilkårsvurdering_periode_særlige_grunner RENAME COLUMN
    annet_særlig_grunn_begrunnelse TO annet_begrunnelse;
