ALTER TABLE statusmelding_buffer DROP COLUMN backfilled;
UPDATE statusmelding_buffer SET lest=true WHERE mottatt::date < '2026-09-02'::date;
