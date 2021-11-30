CREATE TABLE meldingstelling (
    id          UUID PRIMARY KEY,
    ytelsestype VARCHAR NOT NULL,
    type        VARCHAR NOT NULL,
    status      VARCHAR NOT NULL,
    dato        DATE    NOT NULL,
    antall      INT     NOT NULL
);

CREATE UNIQUE INDEX ON meldingstelling (ytelsestype, type, status, dato);
