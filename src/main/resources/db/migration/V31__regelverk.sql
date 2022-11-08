CREATE TABLE regelverk(
    regelverk VARCHAR NOT NULL PRIMARY KEY
);

INSERT INTO regelverk (regelverk) VALUES ('NASJONAL');
INSERT INTO regelverk (regelverk) VALUES ('EØS');

ALTER TABLE behandling
    ADD COLUMN regelverk VARCHAR;

ALTER TABLE behandling
    ADD CONSTRAINT fagsystemsbehandling_regelverk_fkey FOREIGN KEY (regelverk) REFERENCES regelverk;