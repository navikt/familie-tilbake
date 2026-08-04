UPDATE tilbakekreving_vilkårsvurdering_periode p
SET forrige_periode_id=fp.forrige_id,
    vurdering_type=CASE WHEN fp.forrige_id IS NULL THEN 'IKKE_VURDERT' ELSE 'KOPIERT_FRA' END
FROM (SELECT vurdering_ref,
             id,
             LAG(id) OVER (PARTITION BY vurdering_ref ORDER BY periode_fom, id) AS forrige_id
      FROM tilbakekreving_vilkårsvurdering_periode
      WHERE vurdering_ref IN
            ('4029d551-f4c7-4f05-ad30-f48ccc95aefc'::uuid, 'c89b93fe-1f08-4445-9371-82bce0e30e67'::uuid)) fp
WHERE p.id = fp.id;
