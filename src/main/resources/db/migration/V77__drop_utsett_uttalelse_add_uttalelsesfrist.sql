DROP TABLE IF EXISTS tilbakekreving_utsett_uttalelse;

CREATE TABLE tilbakekreving_uttalelsesfrist(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    opprinnelig_frist DATE NOT NULL,
    ny_frist DATE,
    begrunnelse TEXT
);
