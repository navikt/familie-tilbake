ALTER TABLE kravgrunnlag_buffer ADD COLUMN fagsystem_id VARCHAR(255);
UPDATE kravgrunnlag_buffer SET fagsystem_id=(xpath('/urn:detaljertKravgrunnlagMelding/urn:detaljertKravgrunnlag/urn:fagsystemId/text()', kravgrunnlag::xml, ARRAY[ARRAY['urn', 'urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1']]))[1]::text;
ALTER TABLE kravgrunnlag_buffer ALTER COLUMN fagsystem_id SET NOT NULL;
