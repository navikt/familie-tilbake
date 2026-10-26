CREATE TABLE tilbakekreving_bortfalt_kravbrev(
    id UUID NOT NULL PRIMARY KEY,
    sendt_tid DATE NOT NULL,
    journalpost_id VARCHAR(128),
    dokument_info_id VARCHAR(128)
);
