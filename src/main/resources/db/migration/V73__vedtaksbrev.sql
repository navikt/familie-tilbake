CREATE TABLE tilbakekreving_vedtaksbrev(
    id UUID NOT NULL PRIMARY KEY,
    brev_ref UUID NOT NULL REFERENCES tilbakekreving_brev(id) ON DELETE CASCADE,
    sendt_tid DATE NOT NULL,
    journalpost_id VARCHAR(128)
)