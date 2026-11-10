ALTER TABLE kravgrunnlag_buffer
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY;

CREATE INDEX IF NOT EXISTS tilbakekreving_kravgrunnlag_vedtak_id_idx ON tilbakekreving_kravgrunnlag (vedtak_id);
