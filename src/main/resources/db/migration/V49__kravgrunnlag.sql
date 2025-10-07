CREATE TABLE tilbakekreving_kravgrunnlag(
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_id INT NOT NULL REFERENCES tilbakekreving(id),
    vedtak_id BIGINT NOT NULL,
    kravstatuskode VARCHAR(128) NOT NULL,
    fagsystem_vedtaksdato DATE,
    vedtak_gjelder_type VARCHAR(128) NOT NULL,
    vedtak_gjelder_ident VARCHAR(32) NOT NULL,
    utbetales_til_type VARCHAR(128) NOT NULL,
    utbetales_til_ident VARCHAR(32) NOT NULL,
    skal_beregne_renter BOOLEAN NOT NULL,
    ansvarlig_enhet VARCHAR(4) NOT NULL,
    kontrollfelt VARCHAR(64) NOT NULL,
    kravgrunnlag_id VARCHAR(64) NOT NULL,
    referanse VARCHAR(64) NOT NULL
);

CREATE TABLE tilbakekreving_kravgrunnlag_periode(
    id UUID NOT NULL PRIMARY KEY,
    kravgrunnlag_id UUID NOT NULL REFERENCES tilbakekreving_kravgrunnlag(id),
    fom DATE NOT NULL,
    tom DATE NOT NULL,
    månedlig_skattebeløp VARCHAR NOT NULL
);

CREATE TABLE tilbakekreving_kravgrunnlag_beløp(
    id UUID NOT NULL PRIMARY KEY,
    kravgrunnlag_periode_id UUID NOT NULL REFERENCES tilbakekreving_kravgrunnlag_periode(id),
    klassekode VARCHAR(128),
    klassetype VARCHAR(128),
    opprinnelig_utbetalingsbeløp VARCHAR NOT NULL,
    nytt_beløp VARCHAR NOT NULL,
    tilbakekreves_beløp VARCHAR NOT NULL,
    skatteprosent VARCHAR NOT NULL
);

ALTER TABLE tilbakekreving_behandling ADD CONSTRAINT fk_behandling_kravgrunnlag FOREIGN KEY(kravgrunnlag_id) REFERENCES tilbakekreving_kravgrunnlag(id)
