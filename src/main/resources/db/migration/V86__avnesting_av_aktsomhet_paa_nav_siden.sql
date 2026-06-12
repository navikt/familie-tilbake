ALTER TABLE tilbakekreving_vilkårsvurdering_periode ADD COLUMN unnlates VARCHAR(128);

CREATE TABLE tilbakekreving_vilkårsvurdering_periode_mottakers_forståelse(
    id UUID NOT NULL UNIQUE REFERENCES tilbakekreving_vilkårsvurdering_periode(id) ON DELETE CASCADE,
    mottakers_forståelse VARCHAR(128) NOT NULL,
    begrunnelse TEXT NOT NULL
);

UPDATE tilbakekreving_vilkårsvurdering_periode periode
SET unnlates = aktsomhet.unnlates
FROM tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet
WHERE aktsomhet.id = periode.id
  AND aktsomhet.unnlates IS NOT NULL;

ALTER TABLE tilbakekreving_vilkårsvurdering_periode_aktsomhet DROP COLUMN unnlates;

UPDATE tilbakekreving_vilkårsvurdering_periode periode
SET vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD'
FROM tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet
WHERE aktsomhet.id = periode.id
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT'
  AND aktsomhet.type = 'FORSETT';

INSERT INTO tilbakekreving_vilkårsvurdering_periode_mottakers_forståelse (id, mottakers_forståelse, begrunnelse)
SELECT periode.id, 'FORSTOD', aktsomhet.begrunnelse
FROM tilbakekreving_vilkårsvurdering_periode periode
JOIN tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet ON aktsomhet.id = periode.id
WHERE aktsomhet.type = 'FORSETT'
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD';

UPDATE tilbakekreving_vilkårsvurdering_periode periode
SET unnlates = 'SKAL_IKKE_UNNLATES'
FROM tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet
WHERE aktsomhet.id = periode.id
  AND periode.unnlates IS NULL
  AND aktsomhet.type = 'GROV_UAKTSOMHET'
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT';

INSERT INTO tilbakekreving_vilkårsvurdering_periode_mottakers_forståelse (id, mottakers_forståelse, begrunnelse)
SELECT periode.id, 'MÅTTE_FORSTÅ', aktsomhet.begrunnelse
FROM tilbakekreving_vilkårsvurdering_periode periode
JOIN tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet ON aktsomhet.id = periode.id
WHERE aktsomhet.type = 'GROV_UAKTSOMHET'
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT';

INSERT INTO tilbakekreving_vilkårsvurdering_periode_mottakers_forståelse (id, mottakers_forståelse, begrunnelse)
SELECT periode.id, 'BURDE_FORSTÅTT', aktsomhet.begrunnelse
FROM tilbakekreving_vilkårsvurdering_periode periode
JOIN tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet ON aktsomhet.id = periode.id
WHERE aktsomhet.type = 'SIMPEL_UAKTSOMHET'
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT';
