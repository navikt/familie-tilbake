UPDATE statusmelding_buffer s
SET mottatt = COALESCE(
        (SELECT kg.opprettet
         FROM tilbakekreving_kravgrunnlag kg
                  JOIN tilbakekreving_ekstern_fagsak ef ON ef.tilbakekreving_ref = kg.tilbakekreving_id
         WHERE ef.ekstern_id = s.fagsystem_id
           AND kg.kravstatuskode = 'ENDRET'
           AND kg.referanse = (xpath(
                 '/urn:endringKravOgVedtakstatus/urn:kravOgVedtakstatus/urn:referanse/text()',
                 s.statusmelding::xml,
                 ARRAY [ARRAY ['urn', 'urn:no:nav:tilbakekreving:status:v1']]))[1]::text
         ORDER BY kg.opprettet
         LIMIT 1) - interval '12 hours' + CASE s.status
                                              WHEN 'AVSL' THEN interval '2 seconds'
                                              WHEN 'ENDR' THEN interval '1 second'
                                              ELSE interval '0 seconds'
            END,
        s.mottatt)
WHERE s.mottatt <= '2026-09-02'::date;
