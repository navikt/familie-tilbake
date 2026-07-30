ALTER TABLE tilbakekreving_brev
    ADD COLUMN opprettet TIMESTAMP;

UPDATE tilbakekreving_brev brev
SET opprettet = brev_med_opprettet.opprettet
FROM (SELECT brev.id,
             COALESCE(varselbrev.sendt_tid, vedtaksbrev.sendt_tid)::timestamp
                 + ROW_NUMBER() OVER (ORDER BY brev.ctid) *
                   INTERVAL '1 second' AS opprettet
      FROM tilbakekreving_brev brev
               LEFT JOIN tilbakekreving_varselbrev varselbrev ON varselbrev.id = brev.id
               LEFT JOIN tilbakekreving_vedtaksbrev vedtaksbrev ON vedtaksbrev.id = brev.id) brev_med_opprettet
WHERE brev.id = brev_med_opprettet.id;

ALTER TABLE tilbakekreving_brev
    ALTER COLUMN opprettet SET NOT NULL;
