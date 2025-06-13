CREATE TABLE tilbakekreving_behandling (
    tilbakekreving_id UUID PRIMARY KEY,
    behandling_id UUID,
    CONSTRAINT unike_ider UNIQUE (tilbakekreving_id, behandling_id)
);
