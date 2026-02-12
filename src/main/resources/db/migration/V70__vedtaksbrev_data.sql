CREATE TABLE tilbakekreving_behandling_vedtaksbrev
(
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling (id),
    data           JSON NOT NULL
)
