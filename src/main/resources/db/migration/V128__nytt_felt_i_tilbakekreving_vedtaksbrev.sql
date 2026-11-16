ALTER TABLE tilbakekreving_vedtaksbrev ADD COLUMN vedtaksbrevtype VARCHAR(50) NOT NULL DEFAULT 'FØRSTEGANG';
ALTER TABLE tilbakekreving_vedtaksbrev ALTER COLUMN vedtaksbrevtype DROP DEFAULT;
