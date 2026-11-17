DELETE
FROM tilbakekreving_krav_statusmelding
WHERE id IN (
             '08e019e2-e27c-4262-95e2-0d1d15aa3271',
             '28787563-5118-4143-bead-b32b23f8a1ed'
    );

UPDATE tilbakekreving_krav_statusmelding s
SET opprettet = COALESCE(
        (SELECT kg.opprettet
         FROM tilbakekreving_kravgrunnlag kg
         WHERE kg.tilbakekreving_id = s.tilbakekreving_ref
           AND kg.referanse = s.referanse
           AND kg.kravstatuskode = 'ENDRET'
         ORDER BY kg.opprettet
         LIMIT 1) - interval '13 hours',
        s.opprettet)
WHERE s.opprettet <= '2026-09-02'::date
  AND s.kravstatuskode = 'SPERRET';

UPDATE tilbakekreving
SET nåværende_tilstand='TIL_BEHANDLING',
    neste_påminnelse=NOW() - interval '5 hours'
WHERE ((SELECT ekstern_id_fra_oppdrag
        FROM tilbakekreving_ekstern_fagsak
        WHERE tilbakekreving_ref = tilbakekreving.id
          AND ytelse = 'ARBEIDSAVKLARINGSPENGER') = '4SGY5U8'
    OR (SELECT ekstern_id_fra_oppdrag
        FROM tilbakekreving_ekstern_fagsak
        WHERE tilbakekreving_ref = tilbakekreving.id
          AND ytelse = 'TILLEGGSSTØNAD')
           IN ('1392', '1444', '1511', '1621', '1760', '1797', '1835', '1889', '213', '795'))
  AND tilbakekreving.nåværende_tilstand = 'SPERRET';
