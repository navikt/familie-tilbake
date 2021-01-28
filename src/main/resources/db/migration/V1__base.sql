CREATE TABLE fagsak (
    id                UUID PRIMARY KEY,
    versjon           BIGINT                              NOT NULL,
    fagsystem         VARCHAR                             NOT NULL,
    ekstern_fagsak_id VARCHAR,
    status            VARCHAR                             NOT NULL,
    bruker_ident      VARCHAR,
    bruker_sprakkode  VARCHAR      DEFAULT 'NB'           NOT NULL,
    ytelsestype       VARCHAR      DEFAULT 'BA'           NOT NULL,
    opprettet_av      VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid     TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3)
);

COMMENT ON TABLE fagsak
    IS 'Fagsak for tilbakekreving. Alle behandling er koblet mot en fagsak.';

COMMENT ON COLUMN fagsak.id
    IS 'Primary key';

COMMENT ON COLUMN fagsak.fagsystem
    IS 'Fagsystemet som er eier av tilbakekrevingsbehandling';

COMMENT ON COLUMN fagsak.ekstern_fagsak_id
    IS 'Saksnummer (som gsak har mottatt)';

COMMENT ON COLUMN fagsak.status
    IS 'Fk:status fagsak';

COMMENT ON COLUMN fagsak.bruker_ident
    IS 'Fk:Ident på bruker';

COMMENT ON COLUMN fagsak.ytelsestype
    IS 'Fremmednøkkel til kodeverkstabellen som inneholder oversikt over ytelser';

CREATE UNIQUE INDEX ON fagsak (ekstern_fagsak_id);

CREATE INDEX ON fagsak (status);

CREATE INDEX ON fagsak (bruker_ident);

CREATE INDEX ON fagsak (ytelsestype);

CREATE TABLE behandling (
    id                      UUID PRIMARY KEY,
    versjon                 BIGINT                              NOT NULL,
    fagsak_id               UUID                                NOT NULL REFERENCES fagsak,
    status                  VARCHAR                             NOT NULL,
    type                    VARCHAR                             NOT NULL,
    opprettet_dato          DATE         DEFAULT current_date   NOT NULL,
    avsluttet_dato          DATE,
    ansvarlig_saksbehandler VARCHAR,
    ansvarlig_beslutter     VARCHAR,
    behandlende_enhet       VARCHAR,
    behandlende_enhets_navn VARCHAR,
    opprettet_av            VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid           TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3),
    manuelt_opprettet       BOOLEAN                             NOT NULL,
    ekstern_bruk_id         UUID                                NOT NULL,
    saksbehandlingstype     VARCHAR                             NOT NULL
        CONSTRAINT chk_saksbehandlingstype
            CHECK (saksbehandlingstype IN ('ORDINÆR', 'AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP'))
);

COMMENT ON TABLE behandling
    IS 'Behandling av fagsak';

COMMENT ON COLUMN behandling.id
    IS 'Primary key';

COMMENT ON COLUMN behandling.fagsak_id
    IS 'Fk: fagsak fremmednøkkel for kobling til fagsak';

COMMENT ON COLUMN behandling.status
    IS 'Fk: behandlingsstatus fremmednøkkel til tabellen som viser status på behandlinger';

COMMENT ON COLUMN behandling.type
    IS 'Fk: type behandling ';

COMMENT ON COLUMN behandling.opprettet_dato
    IS 'Dato når behandlingen ble opprettet.';

COMMENT ON COLUMN behandling.avsluttet_dato
    IS 'Dato når behandlingen ble avsluttet.';

COMMENT ON COLUMN behandling.ansvarlig_saksbehandler
    IS 'Id til saksbehandler som oppretter forslag til vedtak ved totrinnsbehandling.';

COMMENT ON COLUMN behandling.ansvarlig_beslutter
    IS 'Beslutter som har fattet vedtaket';

COMMENT ON COLUMN behandling.behandlende_enhet
    IS 'Nav-enhet som behandler behandlingen';

COMMENT ON COLUMN behandling.behandlende_enhets_navn
    IS 'Navn på behandlende enhet';

COMMENT ON COLUMN behandling.manuelt_opprettet
    IS 'Angir om behandlingen ble opprettet manuelt. ';

COMMENT ON COLUMN behandling.ekstern_bruk_id
    IS 'Unik uuid for behandling til utvortes bruk';

COMMENT ON COLUMN behandling.saksbehandlingstype
    IS 'Angir hvordan behandlingen saksbehandles ';

CREATE INDEX ON behandling (fagsak_id);

CREATE INDEX ON behandling (status);

CREATE INDEX ON behandling (type);

CREATE UNIQUE INDEX ON behandling (ekstern_bruk_id);

