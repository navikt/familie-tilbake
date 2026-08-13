CREATE TABLE tilbakekreving_kravgrunnlag_forskjell(
    id UUID NOT NULL PRIMARY KEY,
    faktavurdering_periode_ref UUID UNIQUE REFERENCES tilbakekreving_faktavurdering_periode(id) ON DELETE CASCADE,
    vilkårsvurdering_periode_ref UUID UNIQUE REFERENCES tilbakekreving_vilkårsvurdering_periode(id) ON DELETE CASCADE,
    original_periode_fom DATE NOT NULL,
    original_periode_tom DATE NOT NULL,
    endring_i_beløp VARCHAR(128) NOT NULL,
    CHECK (num_nonnulls(faktavurdering_periode_ref, vilkårsvurdering_periode_ref) = 1)
);
