CREATE TABLE tilbakekreving_utsett_uttalelse(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    ny_frist DATE NOT NULL,
    begrunnelse TEXT NOT NULL
);
