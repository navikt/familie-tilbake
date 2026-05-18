DROP TABLE tilbakekreving_påvent;
ALTER TABLE tilbakekreving_behandling ADD COLUMN forrige_behandlingsstatus VARCHAR(255);
ALTER TABLE tilbakekreving_behandling ALTER COLUMN forrige_behandlingsstatus SET NOT NULL;
