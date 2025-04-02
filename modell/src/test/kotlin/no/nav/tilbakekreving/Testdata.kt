package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v2.BrukerDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.Random
import java.util.UUID

fun eksternFagsak() =
    EksternFagsakDto(
        eksternId = "101010",
        ytelsestype = Ytelsestype.BARNETRYGD,
        fagsystem = Fagsystem.BA,
    )

fun bruker() =
    BrukerDto(
        ident = "31126900011",
        språkkode = Språkkode.NB,
    )

fun opprettTilbakekrevingEvent(
    eksternFagsak: EksternFagsakDto = eksternFagsak(),
    opprettelsevalg: Opprettelsevalg = Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL,
) = OpprettTilbakekrevingEvent(
    eksternFagsak = eksternFagsak,
    opprettelsesvalg = opprettelsevalg,
)

fun kravgrunnlag() =
    KravgrunnlagHendelse(
        internId = UUID.randomUUID(),
        vedtakId = BigInteger(128, Random()),
        kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NYTT,
        fagsystemVedtaksdato = LocalDate.now(),
        vedtakGjelder = KravgrunnlagHendelse.Aktør.Person(bruker().ident),
        utbetalesTil = KravgrunnlagHendelse.Aktør.Person(bruker().ident),
        skalBeregneRenter = false,
        ansvarligEnhet = "0425",
        kontrollfelt = UUID.randomUUID().toString(),
        referanse = UUID.randomUUID().toString(),
        kravgrunnlagId = UUID.randomUUID().toString(),
        perioder =
            listOf(
                KravgrunnlagHendelse.Periode(
                    periode =
                        Datoperiode(
                            fom = LocalDate.of(2018, 1, 1),
                            tom = LocalDate.of(2018, 1, 31),
                        ),
                    månedligSkattebeløp = BigDecimal("0.0"),
                    beløp =
                        listOf(
                            KravgrunnlagHendelse.Periode.Beløp(
                                klassekode = "",
                                klassetype = "",
                                opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
                                nyttBeløp = BigDecimal("10000.0"),
                                tilbakekrevesBeløp = BigDecimal("2000.0"),
                                skatteprosent = BigDecimal("0.0"),
                            ),
                        ),
                ),
            ),
    )
