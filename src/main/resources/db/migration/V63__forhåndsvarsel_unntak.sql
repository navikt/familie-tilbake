CREATE TABLE tilbakekreving_forhåndsvarsel_unntak(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    unntak_begrunnelse VARCHAR(128),
    beskrivelse TEXT
);
