ALTER TABLE tilbakekreving ADD COLUMN neste_påminnelse TIMESTAMP DEFAULT now();
ALTER TABLE tilbakekreving ALTER COLUMN neste_påminnelse DROP DEFAULT;
