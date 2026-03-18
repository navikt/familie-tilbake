CREATE TABLE IF NOT EXISTS tilbakekreving_behandlingslogg(
   id UUID PRIMARY KEY,
   tilbakekreving_ref INT NOT NULL,
   behandling_id UUID,
   behandlingsloggstype VARCHAR(100) NOT NULL,
   rolle VARCHAR(50) NOT NULL,
   behandler_ident VARCHAR(100) NOT NULL,
   opprettet_tid TIMESTAMP NOT NULL
);
