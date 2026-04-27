ALTER TABLE tilbakekreving_varselbrev ADD COLUMN dokument_info_id VARCHAR(128);
ALTER TABLE tilbakekreving_vedtaksbrev ADD COLUMN dokument_info_id VARCHAR(128);

ALTER TABLE tilbakekreving_behandlingslogg ADD COLUMN brev_ref UUID REFERENCES tilbakekreving_brev(id) ON DELETE CASCADE;;
