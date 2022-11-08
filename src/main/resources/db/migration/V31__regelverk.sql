CREATE TABLE regelverk(
    regelverk VARCHAR NOT NULL PRIMARY KEY
);

INSERT INTO regelverk (regelverk) VALUES ('NASJONAL');
INSERT INTO regelverk (regelverk) VALUES ('EØS');

ALTER TABLE fagsystemsbehandling
    ADD COLUMN regelverk VARCHAR;

ALTER TABLE fagsystemsbehandling
    ADD CONSTRAINT fagsystemsbehandling_regelverk_fkey FOREIGN KEY (regelverk) REFERENCES regelverk;