CREATE TABLE varselsperiode
(
    id            UUID PRIMARY KEY,
    varsel_id     UUID                                NOT NULL REFERENCES varsel,
    fom           DATE                                NOT NULL,
    tom           DATE                                NOT NULL,
    versjon       INTEGER      DEFAULT 0              NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

COMMENT ON TABLE varselsperiode
    IS 'Feilutbetalingsperiode som brukes i varselbrev';

COMMENT ON COLUMN varselsperiode.id
    IS 'Primary key';

COMMENT ON COLUMN varselsperiode.varsel_id
    IS 'FK: varsel fremmednøkel for kobling til varsel';

COMMENT ON COLUMN varselsperiode.fom
    IS 'Første dag av feilutbetalingsperiode';

COMMENT ON COLUMN varselsperiode.fom
    IS 'Siste dag av feilutbetalingsperiode';

CREATE INDEX ON varselsperiode (varsel_id);

