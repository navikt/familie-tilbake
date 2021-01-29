DROP INDEX fagsak_ekstern_fagsak_id_idx;

CREATE UNIQUE INDEX ON fagsak (ekstern_fagsak_id, ytelsestype);
