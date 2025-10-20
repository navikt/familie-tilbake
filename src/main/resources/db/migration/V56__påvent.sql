CREATE TABLE tilbakekreving_påvent(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    årsak VARCHAR(128),
    utløpsdato DATE,
    begrunnelse VARCHAR(128)
);
