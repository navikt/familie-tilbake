ALTER TABLE tilbakekreving_kravgrunnlag ADD COLUMN kravgrunnlag_hentet_årsak VARCHAR(50) NOT NULL DEFAULT 'DANNING_AV_NYTT_TILBAKEKREVINGSVEDTAK';
ALTER TABLE tilbakekreving_kravgrunnlag ALTER COLUMN kravgrunnlag_hentet_årsak DROP DEFAULT;
ALTER TABLE tilbakekreving_kravgrunnlag DROP COLUMN korrigering;
