ALTER TABLE tilbakekreving_behandling
    ADD COLUMN ansvarlig_saksbehandler_type  VARCHAR(128),
    ADD COLUMN ansvarlig_saksbehandler_ident VARCHAR(64),
    ADD COLUMN enhet_id                      VARCHAR(16),
    ADD COLUMN enhet_navn                    VARCHAR(64)
