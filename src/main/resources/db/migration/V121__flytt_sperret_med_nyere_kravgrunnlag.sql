UPDATE tilbakekreving t
SET nåværende_tilstand = 'AVVENTER_BRUKERINFO',
    neste_påminnelse   = now() - interval '3 hours'
WHERE t.nåværende_tilstand = 'SPERRET'
  AND (SELECT max(kg.opprettet)
       FROM tilbakekreving_kravgrunnlag kg
       WHERE kg.tilbakekreving_id = t.id)
    > (SELECT max(s.opprettet)
       FROM tilbakekreving_krav_statusmelding s
       WHERE s.tilbakekreving_ref = t.id
         AND s.kravstatuskode = 'SPERRET');
