CREATE TABLE IF NOT EXISTS tilbakekreving_behandlingslogg(
   id UUID PRIMARY KEY,
   tilbakekreving_ref INT NOT NULL,
   behandling_id UUID NOT NULL,
   type VARCHAR(100) NOT NULL,
   utfører VARCHAR(50) NOT NULL,
   utfører_ident VARCHAR(100) NOT NULL,
   tittel VARCHAR(255) NOT NULL,
   tekst TEXT,
   steg VARCHAR(100),
   opprettet_tid TIMESTAMP NOT NULL
);
