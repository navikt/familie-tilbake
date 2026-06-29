UPDATE kravgrunnlag_buffer
SET mottatt = TO_TIMESTAMP(
        (xpath(
                '/urn:detaljertKravgrunnlagMelding/urn:detaljertKravgrunnlag/urn:kontrollfelt/text()',
                kravgrunnlag::xml,
                ARRAY [ARRAY ['urn', 'urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1'], ARRAY ['mmel', 'urn:no:nav:tilbakekreving:typer:v1']]
         ))[1]::text,
        'YYYY-MM-DD-HH24.MI.SS.US'
              )
WHERE mottatt < '2026-06-29T13:25:00.00000'::timestamp;
UPDATE kravgrunnlag_buffer SET utenfor_scope=false WHERE utenfor_scope;
