CREATE TABLE tilbakekreving_faktavurdering_feilutbetaling_oppdaget(
    faktavurdering_ref UUID NOT NULL UNIQUE REFERENCES tilbakekreving_faktavurdering(id),
    av VARCHAR(128) NOT NULL,
    beskrivelse TEXT NOT NULL,
    dato DATE NOT NULL
)
