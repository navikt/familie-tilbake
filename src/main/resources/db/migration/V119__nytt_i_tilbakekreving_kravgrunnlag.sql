ALTER TABLE tilbakekreving_kravgrunnlag ADD COLUMN henteårsak VARCHAR(50);
UPDATE tilbakekreving_kravgrunnlag SET henteårsak = 'KORRIGERING_AV_KRAV' WHERE korrigering = true;
ALTER TABLE tilbakekreving_kravgrunnlag DROP COLUMN korrigering;
