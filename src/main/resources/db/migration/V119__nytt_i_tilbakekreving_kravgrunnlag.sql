ALTER TABLE tilbakekreving_kravgrunnlag ADD COLUMN henteårsak VARCHAR(50) NOT NULL DEFAULT 'OMGJØRING_AV_VEDTAK';
ALTER TABLE tilbakekreving_kravgrunnlag ALTER COLUMN henteårsak DROP DEFAULT;
UPDATE tilbakekreving_kravgrunnlag SET henteårsak = 'KORRIGERING_AV_KRAV' WHERE korrigering = true;
ALTER TABLE tilbakekreving_kravgrunnlag DROP COLUMN korrigering;
