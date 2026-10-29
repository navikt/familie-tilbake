CREATE TABLE tilbakekreving_informasjonsbrev(
    id UUID NOT NULL PRIMARY KEY,
    informasjonsbrevtype VARCHAR(128) NOT NULL,
    sendt_tid DATE NOT NULL,
    journalpost_id VARCHAR(128),
    dokument_info_id VARCHAR(128)
);
