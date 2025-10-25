CREATE TABLE tilbakekreving_brevmottaker (
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    aktivert BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE tilbakekreving_registrert_brevmottaker (
    id                   UUID PRIMARY KEY,
    brevmottaker_ref UUID REFERENCES tilbakekreving_brevmottaker(id) ON DELETE CASCADE,
    parent_ref UUID REFERENCES tilbakekreving_registrert_brevmottaker(id) ON DELETE CASCADE,
    mottaker_type        VARCHAR(128) NOT NULL,   -- evt. mottaker_type enum
    navn                 VARCHAR(128),
    person_ident         VARCHAR(128),
    organisasjonsnummer  VARCHAR(128),
    vergetype            VARCHAR(128),
    adresselinje1        VARCHAR(128),
    adresselinje2        VARCHAR(128),
    postnummer           VARCHAR(16),
    poststed             VARCHAR(128),
    landkode             VARCHAR(8)
);

