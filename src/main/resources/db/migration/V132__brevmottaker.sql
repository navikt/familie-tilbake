CREATE TABLE tilbakekreving_brevmottaker (
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_ref INT NOT NULL REFERENCES tilbakekreving(id) ON DELETE CASCADE,
    type VARCHAR(50),
    tilleggsmottaker_type VARCHAR(50),
    land VARCHAR(255),
    addresselinje TEXT,
    postnummer VARCHAR(50),
    navn TEXT,
    mottaker_ident VARCHAR(50)
);

CREATE UNIQUE INDEX tilbakekreving_brevmottaker_hovedmottaker_idx ON tilbakekreving_brevmottaker (tilbakekreving_ref) WHERE tilleggsmottaker_type IS NULL;
CREATE UNIQUE INDEX tilbakekreving_brevmottaker_tilleggsmottaker_idx ON tilbakekreving_brevmottaker (tilbakekreving_ref) WHERE tilleggsmottaker_type IS NOT NULL;
