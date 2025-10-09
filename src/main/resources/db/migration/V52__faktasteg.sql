CREATE TABLE tilbakekreving_faktavurdering(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    årsak_til_feilutbetaling TEXT NOT NULL,
    uttalelse VARCHAR(128) NOT NULL,
    vurdering_av_brukers_uttalelse TEXT
);

CREATE TABLE tilbakekreving_faktavurdering_periode(
    id UUID NOT NULL PRIMARY KEY,
    faktavurdering_ref UUID NOT NULL REFERENCES tilbakekreving_faktavurdering(id),
    periode_fom DATE NOT NULL,
    periode_tom DATE NOT NULL,
    rettslig_grunnlag VARCHAR(128) NOT NULL,
    rettslig_grunnlag_underkategori VARCHAR(128) NOT NULL
);
