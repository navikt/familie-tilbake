CREATE TABLE tilbakekreving_foreslåvedtak(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    vurdert BOOLEAN NOT NULL
)
