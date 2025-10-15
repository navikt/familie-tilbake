CREATE TABLE tilbakekreving_totrinnsvurdering(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    beslutter VARCHAR(128),
    behandler_ident VARCHAR(128)
);

CREATE TABLE tilbakekreving_fattevedtak_vurdering(
    id UUID NOT NULL PRIMARY KEY,
    fattevedtak_ref UUID NOT NULL REFERENCES tilbakekreving_fattevedtak(id),
    behandlingssteg VARCHAR(128) NOT NULL,
    vurdering VARCHAR(128) NOT NULL,
    begrunnelse TEXT
)