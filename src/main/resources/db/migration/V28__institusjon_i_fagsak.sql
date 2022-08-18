
ALTER TABLE fagsak
    ADD COLUMN institusjon_org_nr VARCHAR;

COMMENT ON COLUMN fagsak.institusjon_org_nr
    IS 'Organisasjonsnummer for institusjon';

ALTER TABLE fagsak
    ADD COLUMN institusjon_navn VARCHAR;

COMMENT ON COLUMN fagsak.institusjon_navn
    IS 'Navn på intitusjon';
