UPDATE tilbakekreving_behandling tb
SET revurderingsårsak=NULL
WHERE revurderingsårsak = 'REVURDERING_OPPLYSNINGER_OM_VILKÅR'
  AND (SELECT count(1) FROM tilbakekreving_behandling tb2 WHERE tb2.tilbakekreving_id = tb.tilbakekreving_id) = 1;
