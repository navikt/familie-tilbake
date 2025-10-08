CREATE TABLE tilbakekreving_ekstern_fagsak(
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_ref INT NOT NULL REFERENCES tilbakekreving(id),
    ekstern_id VARCHAR(64) NOT NULL,
    ytelse VARCHAR(128) NOT NULL
);

CREATE TABLE tilbakekreving_ekstern_fagsak_behandling(
    id UUID NOT NULL PRIMARY KEY,
    ekstern_fagsak_ref UUID NOT NULL REFERENCES tilbakekreving_ekstern_fagsak(id),
    type VARCHAR(128) NOT NULL,
    ekstern_id VARCHAR(128) NOT NULL,
    revurderingsårsak VARCHAR(128),
    årsak_til_feilutbetaling TEXT,
    vedtaksdato DATE
);

CREATE TABLE tilbakekreving_ekstern_fagsak_behandling_utvidet_periode(
    id UUID NOT NULL PRIMARY KEY,
    ekstern_fagsak_behandling_ref UUID NOT NULL REFERENCES tilbakekreving_ekstern_fagsak_behandling(id),
    kravgrunnlag_periode_fom DATE NOT NULL,
    kravgrunnlag_periode_tom DATE NOT NULL,
    vedtaksperiode_fom DATE NOT NULL,
    vedtaksperiode_tom DATE NOT NULL
);
