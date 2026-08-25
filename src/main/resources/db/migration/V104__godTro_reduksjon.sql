CREATE TABLE tilbakekreving_vilkårsvurdering_periode_relevante_momenter (
    id UUID NOT NULL UNIQUE REFERENCES tilbakekreving_vilkårsvurdering_periode(id) ON DELETE CASCADE,
    begrunnelse TEXT NOT NULL,
    annet_relevant_moment_begrunnelse TEXT,
    skal_reduseres VARCHAR(128) NOT NULL,
    reduksjon_prosent INT,
    relevante_momenter VARCHAR(128)[]
);
