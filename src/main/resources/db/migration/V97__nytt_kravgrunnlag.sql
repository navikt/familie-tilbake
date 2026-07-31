ALTER TABLE tilbakekreving_behandling ADD COLUMN nytt_kravgrunnlag_ref UUID REFERENCES tilbakekreving_kravgrunnlag(id);
ALTER TABLE kravgrunnlag_buffer DROP COLUMN utenfor_scope;
