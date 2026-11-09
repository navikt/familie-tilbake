UPDATE tilbakekreving_krav_statusmelding s
SET opprettet = COALESCE(
        COALESCE(
                (SELECT kg.opprettet
                 FROM tilbakekreving_kravgrunnlag kg
                 WHERE kg.tilbakekreving_id = s.tilbakekreving_ref
                   AND kg.referanse = s.referanse
                   AND kg.kravstatuskode = 'ENDRET'
                 ORDER BY kg.opprettet
                 LIMIT 1),
                (SELECT kg.opprettet
                 FROM tilbakekreving_kravgrunnlag kg
                 WHERE kg.tilbakekreving_id = s.tilbakekreving_ref
                   AND kg.kravstatuskode = 'ENDRET'
                 ORDER BY kg.opprettet DESC
                 LIMIT 1)
        ) - interval '12 hours' + CASE s.kravstatuskode
                                      WHEN 'AVSLUTTET' THEN interval '2 seconds'
                                      WHEN 'ENDRET' THEN interval '1 second'
                                      ELSE interval '0 seconds'
            END,
        s.opprettet)
WHERE s.opprettet <= '2026-09-02'::date;
