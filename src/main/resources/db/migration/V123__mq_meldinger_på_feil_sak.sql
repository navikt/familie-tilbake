UPDATE tilbakekreving_behandling SET kravgrunnlag_id='7db6e681-d092-4f9f-bd98-4dcb0101516f' WHERE kravgrunnlag_id='453960f9-b1f5-4b36-9794-5dac6d2f117e';
DELETE
FROM tilbakekreving_kravgrunnlag_beløp
WHERE kravgrunnlag_periode_id IN (SELECT id
                                  FROM tilbakekreving_kravgrunnlag_periode
                                  WHERE kravgrunnlag_id = '453960f9-b1f5-4b36-9794-5dac6d2f117e');
DELETE FROM tilbakekreving_kravgrunnlag_periode WHERE kravgrunnlag_id = '453960f9-b1f5-4b36-9794-5dac6d2f117e';
DELETE FROM tilbakekreving_kravgrunnlag WHERE id = '453960f9-b1f5-4b36-9794-5dac6d2f117e';


DELETE
FROM tilbakekreving_ekstern_fagsak_behandling_utvidet_periode
WHERE ekstern_fagsak_behandling_ref IN (SELECT id
                                        FROM tilbakekreving_ekstern_fagsak_behandling
                                        WHERE ekstern_fagsak_ref = '7786766d-257c-4207-ae3d-18106e51b548'
                                          AND ekstern_id = 'TLo4vxyvQjW/Gybzei5dIg=='
                                          AND id != 'ee4aa67b-ceed-4c79-8a8d-dc05b710b425');
DELETE FROM tilbakekreving_ekstern_fagsak_behandling
WHERE ekstern_fagsak_ref = '7786766d-257c-4207-ae3d-18106e51b548'
  AND ekstern_id = 'TLo4vxyvQjW/Gybzei5dIg=='
  AND id != 'ee4aa67b-ceed-4c79-8a8d-dc05b710b425';

UPDATE tilbakekreving_behandling SET kravgrunnlag_id='13bc5e98-dcb1-4c10-a4de-de9e35d77f16' WHERE kravgrunnlag_id='af2245d8-eb33-4a4b-a506-757132959811';
DELETE
FROM tilbakekreving_kravgrunnlag_beløp
WHERE kravgrunnlag_periode_id IN (SELECT id
                                  FROM tilbakekreving_kravgrunnlag_periode
                                  WHERE kravgrunnlag_id = 'af2245d8-eb33-4a4b-a506-757132959811');
DELETE FROM tilbakekreving_kravgrunnlag_periode WHERE kravgrunnlag_id = 'af2245d8-eb33-4a4b-a506-757132959811';
DELETE FROM tilbakekreving_kravgrunnlag WHERE id = 'af2245d8-eb33-4a4b-a506-757132959811';

DELETE
FROM tilbakekreving_ekstern_fagsak_behandling_utvidet_periode
WHERE ekstern_fagsak_behandling_ref IN (SELECT id
                                        FROM tilbakekreving_ekstern_fagsak_behandling
                                        WHERE ekstern_fagsak_ref = '11bd1afb-5c9e-4c23-bac8-f2f9d47e62ab'
                                          AND ekstern_id = 'beh_01KPTF4X8Z738XA43RG9A7400M'
                                          AND id != '42d4d404-c5b8-4975-96ec-377b5acf3869');
DELETE FROM tilbakekreving_ekstern_fagsak_behandling
WHERE ekstern_fagsak_ref = '11bd1afb-5c9e-4c23-bac8-f2f9d47e62ab'
  AND ekstern_id = 'beh_01KPTF4X8Z738XA43RG9A7400M'
  AND id != '42d4d404-c5b8-4975-96ec-377b5acf3869';
