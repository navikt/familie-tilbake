CREATE TABLE tilbakekreving_foreldelsesvurdering(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id)
);

CREATE TABLE tilbakekreving_foreldelsesvurdering_periode(
    id UUID NOT NULL PRIMARY KEY,
    foreldelsesvurdering_ref UUID NOT NULL REFERENCES tilbakekreving_foreldelsesvurdering(id),
    periode_fom DATE NOT NULL,
    periode_tom DATE NOT NULL,
    vurdering VARCHAR(128) NOT NULL,
    begrunnelse TEXT,
    frist DATE,
    oppdaget DATE
)
