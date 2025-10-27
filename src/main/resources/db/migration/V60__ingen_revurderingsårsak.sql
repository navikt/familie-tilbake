ALTER TABLE tilbakekreving_behandling RENAME COLUMN årsak TO revurderingsårsak;
ALTER TABLE tilbakekreving_behandling RENAME COLUMN behandlingstype TO type;
ALTER TABLE tilbakekreving_behandling ALTER COLUMN revurderingsårsak DROP NOT NULL;

