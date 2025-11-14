CREATE TABLE tilbakekreving_brukeruttalelse(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    uttalelse_vurdering VARCHAR(32),
    beskrivelse_ved_nei_eller_utsett_frist TEXT,
    utsett_frist DATE
);

CREATE TABLE tilbakekreving_uttalelse_informasjon(
    id UUID NOT NULL PRIMARY KEY,
    brukeruttalelse_ref UUID NOT NULL REFERENCES tilbakekreving_brukeruttalelse(id),
    uttalelsesdato DATE NOT NULL,
    hvor_brukeren_uttalet_seg VARCHAR(128) NOT NULL,
    uttalelse_beskrivelse TEXT NOT NULL
)
