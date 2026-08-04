UPDATE tilbakekreving_vilkårsvurdering_periode
SET vurdering_type='KOPIERT_VURDERING'
WHERE vurdering_type = 'KOPIERT_FRA';
