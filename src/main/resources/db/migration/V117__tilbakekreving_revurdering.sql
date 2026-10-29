CREATE TABLE tilbakekreving_revurdering (
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    revurderingsårsak_type VARCHAR(255) NOT NULL,
    instans VARCHAR(255),
    beskrivelse TEXT
);

ALTER TABLE tilbakekreving_behandling DROP COLUMN revurderingsårsak;
