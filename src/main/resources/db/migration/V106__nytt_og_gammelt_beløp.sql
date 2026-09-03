ALTER TABLE tilbakekreving_kravgrunnlag_forskjell RENAME COLUMN endring_i_beløp TO nytt_beløp;
ALTER TABLE tilbakekreving_kravgrunnlag_forskjell ADD COLUMN gammelt_beløp VARCHAR(128);
UPDATE tilbakekreving_kravgrunnlag_forskjell SET gammelt_beløp='0' WHERE type='JustertBeløp';
