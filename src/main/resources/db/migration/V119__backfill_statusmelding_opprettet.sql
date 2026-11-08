UPDATE tilbakekreving_krav_statusmelding s
SET opprettet = COALESCE((SELECT kg.opprettet
                          FROM tilbakekreving_kravgrunnlag kg
                          WHERE kg.tilbakekreving_id = s.tilbakekreving_ref
                            AND kg.referanse = s.referanse
                            AND kg.kravstatuskode = 'ENDRET'
                          ORDER BY kg.opprettet
                          LIMIT 1) - interval '12 hours', s.opprettet)
WHERE s.opprettet <= '2026-09-02'::date;

UPDATE tilbakekreving_krav_statusmelding s
SET opprettet = COALESCE((SELECT kg.opprettet
                          FROM tilbakekreving_kravgrunnlag kg
                          WHERE kg.tilbakekreving_id = s.tilbakekreving_ref
                            AND kg.kravstatuskode = 'ENDRET'
                          ORDER BY kg.opprettet DESC
                          LIMIT 1) - interval '12 hours', s.opprettet)
WHERE s.opprettet <= '2026-09-02'::date;
