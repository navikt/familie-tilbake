DROP TABLE tilbakekreving_behandling;
ALTER TABLE tilbakekreving_snapshot ALTER id TYPE VARCHAR(128);

CREATE TABLE tilbakekreving
(
    id INT NOT NULL PRIMARY KEY,
    nåværende_tilstand VARCHAR(128) NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    opprettelsesvalg VARCHAR(128) NOT NULL
);

CREATE SEQUENCE tilbakekreving_id OWNED BY tilbakekreving.id;
