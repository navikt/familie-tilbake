CREATE TABLE tilbakekreving_vilkårsvurdering(
    id UUID NOT NULL PRIMARY KEY,
    behandling_ref UUID NOT NULL REFERENCES tilbakekreving_behandling(id)
);

CREATE TABLE tilbakekreving_vilkårsvurdering_periode(
    id UUID NOT NULL PRIMARY KEY,
    vurdering_ref UUID NOT NULL REFERENCES tilbakekreving_vilkårsvurdering(id),
    periode_fom DATE NOT NULL,
    periode_tom DATE NOT NULL,
    vurdering_type VARCHAR(128) NOT NULL,
    vilkår_for_tilbakekreving TEXT,
    feilaktig_eller_mangelfull VARCHAR(128)
);

CREATE TABLE tilbakekreving_vilkårsvurdering_periode_god_tro(
    id UUID NOT NULL UNIQUE REFERENCES tilbakekreving_vilkårsvurdering_periode(id) ON DELETE CASCADE,
    begrunnelse TEXT NOT NULL,
    beløp_i_behold VARCHAR(128) NOT NULL,
    beløp VARCHAR(128)
);

CREATE TABLE tilbakekreving_vilkårsvurdering_periode_aktsomhet(
    id UUID NOT NULL UNIQUE REFERENCES tilbakekreving_vilkårsvurdering_periode(id) ON DELETE CASCADE,
    type VARCHAR(128) NOT NULL,
    begrunnelse TEXT NOT NULL,
    unnlates VARCHAR(128)
);

CREATE TABLE tilbakekreving_vilkårsvurdering_periode_særlige_grunner(
    id UUID NOT NULL UNIQUE REFERENCES tilbakekreving_vilkårsvurdering_periode(id) ON DELETE CASCADE,
    begrunnelse TEXT NOT NULL,
    annet_særlig_grunn_begrunnelse TEXT,
    skal_reduseres VARCHAR(128) NOT NULL,
    reduksjon_prosent INT,
    særlige_grunner VARCHAR(128)[]
);
