ALTER TABLE tilbakekreving_varselbrev
    ADD COLUMN kravgrunnlag_ref UUID;

UPDATE tilbakekreving_varselbrev vb
SET kravgrunnlag_ref = b.kravgrunnlag_ref
    FROM tilbakekreving_brev b
WHERE vb.brev_ref = b.id;

ALTER TABLE tilbakekreving_varselbrev
    ALTER COLUMN kravgrunnlag_ref SET NOT NULL;

ALTER TABLE tilbakekreving_brev
DROP COLUMN kravgrunnlag_ref;