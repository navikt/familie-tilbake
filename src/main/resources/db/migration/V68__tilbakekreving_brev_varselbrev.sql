CREATE TABLE tilbakekreving_brev(
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_ref INT NOT NULL REFERENCES tilbakekreving(id),
    brevtype VARCHAR(128) NOT NULL,
    kravgrunnlag_ref UUID NOT NULL
);

CREATE TABLE tilbakekreving_varselbrev(
    id UUID NOT NULL PRIMARY KEY,
    brev_ref UUID NOT NULL REFERENCES tilbakekreving_brev(id) ON DELETE CASCADE,
    sendt_tid DATE NOT NULL,
    frist_for_uttalelse DATE,
    journalpost_id VARCHAR(128),
    ansvarlig_saksbehandler_ident VARCHAR(128),
    tekst_fra_saksbehandler TEXT
)
