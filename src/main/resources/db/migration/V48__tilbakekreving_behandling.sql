CREATE TABLE tilbakekreving_behandling(
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_id INT NOT NULL REFERENCES tilbakekreving,
    behandlingstype VARCHAR(128) NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    sist_endret TIMESTAMP NOT NULL,
    årsak VARCHAR(128) NOT NULL,
    ekstern_fagsak_behandling_id UUID NOT NULL,
    kravgrunnlag_id UUID NOT NULL
)
