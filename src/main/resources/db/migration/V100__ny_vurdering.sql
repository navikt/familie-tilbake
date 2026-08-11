ALTER TABLE tilbakekreving_faktavurdering ALTER COLUMN trenger_ny_vurdering DROP NOT NULL;
ALTER TABLE tilbakekreving_faktavurdering ALTER COLUMN trenger_ny_vurdering TYPE VARCHAR(128) USING CASE WHEN trenger_ny_vurdering THEN 'Underkjent' END;
ALTER TABLE tilbakekreving_faktavurdering RENAME COLUMN trenger_ny_vurdering TO tilbakeført;

ALTER TABLE tilbakekreving_forhåndsvarsel_unntak ALTER COLUMN trenger_ny_vurdering DROP NOT NULL;
ALTER TABLE tilbakekreving_forhåndsvarsel_unntak ALTER COLUMN trenger_ny_vurdering TYPE VARCHAR(128) USING CASE WHEN trenger_ny_vurdering THEN 'Underkjent' END;
ALTER TABLE tilbakekreving_forhåndsvarsel_unntak RENAME COLUMN trenger_ny_vurdering TO tilbakeført;

ALTER TABLE tilbakekreving_brukeruttalelse ALTER COLUMN trenger_ny_vurdering DROP NOT NULL;
ALTER TABLE tilbakekreving_brukeruttalelse ALTER COLUMN trenger_ny_vurdering TYPE VARCHAR(128) USING CASE WHEN trenger_ny_vurdering THEN 'Underkjent' END;
ALTER TABLE tilbakekreving_brukeruttalelse RENAME COLUMN trenger_ny_vurdering TO tilbakeført;

ALTER TABLE tilbakekreving_foreldelsesvurdering ALTER COLUMN trenger_ny_vurdering DROP NOT NULL;
ALTER TABLE tilbakekreving_foreldelsesvurdering ALTER COLUMN trenger_ny_vurdering TYPE VARCHAR(128) USING CASE WHEN trenger_ny_vurdering THEN 'Underkjent' END;
ALTER TABLE tilbakekreving_foreldelsesvurdering RENAME COLUMN trenger_ny_vurdering TO tilbakeført;

ALTER TABLE tilbakekreving_vilkårsvurdering ALTER COLUMN trenger_ny_vurdering DROP NOT NULL;
ALTER TABLE tilbakekreving_vilkårsvurdering ALTER COLUMN trenger_ny_vurdering TYPE VARCHAR(128) USING CASE WHEN trenger_ny_vurdering THEN 'Underkjent' END;
ALTER TABLE tilbakekreving_vilkårsvurdering RENAME COLUMN trenger_ny_vurdering TO tilbakeført;

ALTER TABLE tilbakekreving_foreslåvedtak ALTER COLUMN trenger_ny_vurdering DROP NOT NULL;
ALTER TABLE tilbakekreving_foreslåvedtak ALTER COLUMN trenger_ny_vurdering TYPE VARCHAR(128) USING CASE WHEN trenger_ny_vurdering THEN 'Underkjent' END;
ALTER TABLE tilbakekreving_foreslåvedtak RENAME COLUMN trenger_ny_vurdering TO tilbakeført;
