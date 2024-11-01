ALTER TABLE vedtaksbrevsoppsummering DROP COLUMN skal_sammenslaa_perioder;
ALTER TABLE vedtaksbrevsoppsummering
    ADD COLUMN skal_sammenslaa_perioder VARCHAR NOT NULL DEFAULT 'PRE_IMPLEMENTASJON';
