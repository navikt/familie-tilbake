CREATE TABLE ekstern_behandling (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    behandling_id UUID                                NOT NULL REFERENCES behandling,
    aktiv         BOOLEAN      DEFAULT TRUE           NOT NULL,
    ekstern_id    VARCHAR                             NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    UNIQUE (behandling_id, ekstern_id)
);

COMMENT ON TABLE ekstern_behandling
    IS 'Referanse til ekstern behandling';

COMMENT ON COLUMN ekstern_behandling.id
    IS 'Primary key';

COMMENT ON COLUMN ekstern_behandling.behandling_id
    IS 'Fk: behandling fremmednøkkel for kobling til intern behandling';

COMMENT ON COLUMN ekstern_behandling.aktiv
    IS 'Angir om ekstern behandling data er gjeldende';

COMMENT ON COLUMN ekstern_behandling.ekstern_id
    IS 'ekstern_id;referanse. Peker på referanse-feltet i kravgrunnlaget, og kommer opprinnelig fra fagsystemet.';

CREATE INDEX ON ekstern_behandling (behandling_id);

CREATE INDEX ON ekstern_behandling (ekstern_id);

CREATE TABLE varsel (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    behandling_id           UUID                                NOT NULL REFERENCES behandling,
    aktiv                   BOOLEAN                             NOT NULL,
    varseltekst             VARCHAR                             NOT NULL,
    varselbelop             BIGINT,
    revurderingsvedtaksdato DATE                                NOT NULL,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
);

COMMENT ON TABLE varsel
    IS 'Tabell for å lagre varsel info';

COMMENT ON COLUMN varsel.id
    IS 'Primary key';

COMMENT ON COLUMN varsel.behandling_id
    IS 'Fk: behandling fremmednøkkel for tilknyttet behandling';

COMMENT ON COLUMN varsel.aktiv
    IS 'Angir status av varsel';

COMMENT ON COLUMN varsel.varseltekst
    IS 'Fritekst som brukes i varselbrev';

COMMENT ON COLUMN varsel.varselbelop
    IS 'Beløp som brukes i varselbrev';

COMMENT ON COLUMN varsel.revurderingsvedtaksdato
    IS 'vedtaksdato av fagsystemsrevurdering. Brukes av varselbrev';

CREATE INDEX ON varsel (behandling_id);

CREATE TABLE varselsperiode (
    id            UUID PRIMARY KEY,
    versjon       BIGINT                              NOT NULL,
    varsel_id     UUID                                NOT NULL REFERENCES varsel,
    fom           DATE                                NOT NULL,
    tom           DATE                                NOT NULL,
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

COMMENT ON COLUMN varselsperiode.versjon
    IS 'Bruker for optimistisk låsing';

CREATE INDEX ON varselsperiode (varsel_id);

CREATE TABLE verge (
    id            UUID PRIMARY KEY,
    behandling_id UUID                                NOT NULL REFERENCES behandling,
    versjon       BIGINT                              NOT NULL,
    ident         VARCHAR,
    gyldig_fom    DATE                                NOT NULL,
    gyldig_tom    DATE                                NOT NULL,
    aktiv         BOOLEAN                             NOT NULL,
    type          VARCHAR                             NOT NULL,
    org_nr        VARCHAR,
    navn          VARCHAR                             NOT NULL,
    kilde         VARCHAR                             NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    begrunnelse   VARCHAR
);

COMMENT ON TABLE verge
    IS 'Informasjon om verge';

COMMENT ON COLUMN verge.id
    IS 'Primary key';

COMMENT ON COLUMN verge.ident
    IS 'Aktørid av verge person';

COMMENT ON COLUMN verge.gyldig_fom
    IS 'Hvis fullmakt er begrenset i periode, dato for når fullmakten er gyldig fra';

COMMENT ON COLUMN verge.gyldig_tom
    IS 'Hvis fullmakt er begrenset i periode, dato for når fullmakten er gyldig til';

COMMENT ON COLUMN verge.type
    IS 'Type verge';

COMMENT ON COLUMN verge.org_nr
    IS 'Vergens organisasjonsnummer';

COMMENT ON COLUMN verge.navn
    IS 'Navn på vergen, som tastet inn av saksbehandler';

COMMENT ON COLUMN verge.kilde
    IS 'Opprinnelsen av verge.enten fpsak hvis det kopierte fra fpsak eller fptilbake';

COMMENT ON COLUMN verge.begrunnelse
    IS 'Begrunnelse for verge';
