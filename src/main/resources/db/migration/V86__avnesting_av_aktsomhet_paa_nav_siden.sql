ALTER TABLE tilbakekreving_vilkårsvurdering_periode ADD COLUMN unnlates VARCHAR(128);
ALTER TABLE tilbakekreving_vilkårsvurdering_periode ADD COLUMN mottakers_forståelse VARCHAR(128);

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

UPDATE tilbakekreving_vilkårsvurdering_periode periode
SET unnlates = 'SKAL_IKKE_UNNLATES',
    mottakers_forståelse = 'MÅTTE_FORSTÅ'
FROM tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet
WHERE aktsomhet.id = periode.id
  AND periode.unnlates IS NULL
  AND aktsomhet.type = 'GROV_UAKTSOMHET'
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT';

UPDATE tilbakekreving_vilkårsvurdering_periode periode
SET mottakers_forståelse = 'BURDE_FORSTÅTT'
FROM tilbakekreving_vilkårsvurdering_periode_aktsomhet aktsomhet
WHERE aktsomhet.id = periode.id
  AND aktsomhet.type = 'SIMPEL_UAKTSOMHET'
  AND periode.vurdering_type = 'IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT';
