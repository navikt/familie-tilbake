ALTER TABLE tilbakekreving_kravgrunnlag ADD COLUMN korrigering BOOLEAN DEFAULT false;
ALTER TABLE tilbakekreving_kravgrunnlag ALTER COLUMN korrigering DROP DEFAULT;
