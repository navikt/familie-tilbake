ALTER TABLE tilbakekreving_ekstern_fagsak RENAME COLUMN ekstern_id TO ekstern_id_fra_oppdrag;
ALTER TABLE tilbakekreving_ekstern_fagsak ADD COLUMN ekstern_id VARCHAR(128);
