ALTER TABLE tilbakekreving_faktavurdering ADD COLUMN trenger_ny_vurdering BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tilbakekreving_forhåndsvarsel_unntak ADD COLUMN trenger_ny_vurdering BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tilbakekreving_brukeruttalelse ADD COLUMN trenger_ny_vurdering BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tilbakekreving_foreldelsesvurdering ADD COLUMN trenger_ny_vurdering BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tilbakekreving_vilkårsvurdering ADD COLUMN trenger_ny_vurdering BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tilbakekreving_foreslåvedtak ADD COLUMN trenger_ny_vurdering BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tilbakekreving_faktavurdering ALTER COLUMN trenger_ny_vurdering DROP DEFAULT;
ALTER TABLE tilbakekreving_forhåndsvarsel_unntak ALTER COLUMN trenger_ny_vurdering DROP DEFAULT;
ALTER TABLE tilbakekreving_brukeruttalelse ALTER COLUMN trenger_ny_vurdering DROP DEFAULT;
ALTER TABLE tilbakekreving_foreldelsesvurdering ALTER COLUMN trenger_ny_vurdering DROP DEFAULT;
ALTER TABLE tilbakekreving_vilkårsvurdering ALTER COLUMN trenger_ny_vurdering DROP DEFAULT;
ALTER TABLE tilbakekreving_foreslåvedtak ALTER COLUMN trenger_ny_vurdering DROP DEFAULT;
