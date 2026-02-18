CREATE TABLE tilbakekreving_behandling_vedtaksbrev
(
    behandling_ref UUID      NOT NULL REFERENCES tilbakekreving_behandling (id),
    sist_oppdatert TIMESTAMP NOT NULL,
    data           JSON      NOT NULL
)
