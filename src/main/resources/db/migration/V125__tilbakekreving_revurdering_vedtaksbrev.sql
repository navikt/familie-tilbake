CREATE TABLE tilbakekreving_revurdering_behandling_vedtaksbrev (
    behandling_ref UUID      NOT NULL REFERENCES tilbakekreving_behandling (id),
    sist_oppdatert TIMESTAMP NOT NULL,
    data           text      NOT NULL
);
