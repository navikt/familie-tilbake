CREATE TABLE tilbakekreving_brukeruttalelse(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    uttalelse_vurdering VARCHAR(32),
    kommentar TEXT
);

CREATE TABLE tilbakekreving_uttalelse_informasjon(
    id UUID NOT NULL PRIMARY KEY,
    brukeruttalelse_ref UUID NOT NULL REFERENCES tilbakekreving_brukeruttalelse(id),
    uttalelsesdato DATE NOT NULL,
    hvor_brukeren_uttalet_seg VARCHAR(128) NOT NULL,
    uttalelse_beskrivelse TEXT NOT NULL
);

CREATE TABLE tilbakekreving_utsett_uttalelse(
    id UUID NOT NULL PRIMARY KEY,
    brukeruttalelse_ref UUID NOT NULL REFERENCES tilbakekreving_brukeruttalelse(id),
    ny_frist DATE NOT NULL,
    begrunnelse TEXT NOT NULL
);
