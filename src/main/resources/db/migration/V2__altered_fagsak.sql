DROP INDEX fagsak_ekstern_fagsak_id_idx;
DROP INDEX fagsak_ytelsestype_idx;

CREATE UNIQUE INDEX ON fagsak (ekstern_fagsak_id, ytelsestype);
