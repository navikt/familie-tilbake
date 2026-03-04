DELETE FROM tilbakekreving_brev
WHERE id IN (
    SELECT tb.id
    FROM tilbakekreving_brev tb
             JOIN tilbakekreving_ekstern_fagsak tef
                  ON tef.tilbakekreving_ref = tb.tilbakekreving_ref
    WHERE tef.ekstern_id = '4LUUX0W'
);
