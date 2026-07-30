ALTER TABLE tilbakekreving_ekstern_fagsak_behandling
    ADD COLUMN opprettet TIMESTAMP;

UPDATE tilbakekreving_ekstern_fagsak_behandling ekstern_fagsak_behandling
SET opprettet = ekstern_fagsak_behandling_med_opprettet.opprettet
FROM (SELECT id,
             COALESCE(vedtaksdato, CURRENT_DATE)::timestamp
                 + ROW_NUMBER() OVER (ORDER BY ctid) *
                   INTERVAL '1 second' AS opprettet
      FROM tilbakekreving_ekstern_fagsak_behandling) ekstern_fagsak_behandling_med_opprettet
WHERE ekstern_fagsak_behandling.id = ekstern_fagsak_behandling_med_opprettet.id;

ALTER TABLE tilbakekreving_ekstern_fagsak_behandling
    ALTER COLUMN opprettet SET NOT NULL;
