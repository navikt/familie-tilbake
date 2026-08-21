ALTER TABLE tilbakekreving_kravgrunnlag_forskjell
    DROP CONSTRAINT IF EXISTS tilbakekreving_kravgrunnlag_forskjell_check,
    ALTER COLUMN original_periode_fom DROP NOT NULL,
    ALTER COLUMN original_periode_tom DROP NOT NULL,
    ALTER COLUMN endring_i_beløp DROP NOT NULL,
    ADD COLUMN ny_periode_fom DATE,
    ADD COLUMN ny_periode_tom DATE,
    ADD COLUMN type VARCHAR(50),
    ADD COLUMN foreldelsesvurdering_periode_ref UUID UNIQUE REFERENCES tilbakekreving_foreldelsesvurdering_periode(id) ON DELETE CASCADE,
    ADD CONSTRAINT tilbakekreving_kravgrunnlag_forskjell_refs CHECK (num_nonnulls(faktavurdering_periode_ref, vilkårsvurdering_periode_ref, foreldelsesvurdering_periode_ref) = 1);

UPDATE tilbakekreving_kravgrunnlag_forskjell
SET type = 'JustertBeløp';

ALTER TABLE tilbakekreving_kravgrunnlag_forskjell
    ALTER COLUMN type SET NOT NULL;
