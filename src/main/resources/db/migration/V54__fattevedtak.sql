CREATE TABLE tilbakekreving_totrinnsvurdering(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id),
    beslutter_type VARCHAR(128),
    beslutter_ident VARCHAR(128)
);

CREATE TABLE tilbakekreving_totrinnsvurdering_vurdertsteg(
    id UUID NOT NULL PRIMARY KEY,
    totrinnsvurdering_ref UUID NOT NULL REFERENCES tilbakekreving_totrinnsvurdering(id),
    behandlingssteg VARCHAR(128) NOT NULL,
    vurdering VARCHAR(128) NOT NULL,
    begrunnelse TEXT
)