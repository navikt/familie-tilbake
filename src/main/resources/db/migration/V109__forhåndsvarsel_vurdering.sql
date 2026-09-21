CREATE TABLE tilbakekreving_forhåndsvarsel
(
    id UUID NOT NULL PRIMARY KEY REFERENCES tilbakekreving_behandling(id),
    vurderingstype VARCHAR(64) NOT NULL,
    tilbakeført VARCHAR(128)
);

INSERT INTO tilbakekreving_forhåndsvarsel(id, vurderingstype, tilbakeført)
SELECT behandling.id,
       CASE
           WHEN unntak.behandling_ref IS NOT NULL THEN 'UNNTAK'
           WHEN frist.behandling_ref IS NOT NULL THEN 'VARSEL_SENDT'
           ELSE 'IKKE_VURDERT'
       END,
       unntak.tilbakeført
FROM tilbakekreving_behandling behandling
LEFT JOIN tilbakekreving_forhåndsvarsel_unntak unntak ON unntak.behandling_ref = behandling.id
LEFT JOIN tilbakekreving_uttalelsesfrist frist ON frist.behandling_ref = behandling.id;
