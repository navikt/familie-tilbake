package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v2.BrukerDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
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

fun kravgrunnlag(
    perioder: List<KravgrunnlagHendelse.Periode> = listOf(kravgrunnlagPeriode()),
) =
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
        perioder = perioder,
    )

fun kravgrunnlagPeriode(
    periode: Datoperiode = 1.januar til 31.januar,
    kravgrunnlagBeløp: List<KravgrunnlagHendelse.Periode.Beløp> = kravgrunnlagBeløp(),
) =
    KravgrunnlagHendelse.Periode(
        periode = periode,
        månedligSkattebeløp = BigDecimal("0.0"),
        beløp = kravgrunnlagBeløp,
    )

fun kravgrunnlagBeløp() =
    listOf(
        KravgrunnlagHendelse.Periode.Beløp(
            klassekode = "",
            klassetype = "",
            opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
            nyttBeløp = BigDecimal("10000.0"),
            tilbakekrevesBeløp = BigDecimal("2000.0"),
            skatteprosent = BigDecimal("0.0"),
        ),
    )

fun fagsysteminfoHendelse() =
    FagsysteminfoHendelse(
        eksternId = UUID.randomUUID().toString(),
        revurderingsårsak = "Revurderingsårsak",
        revurderingsresultat = "Revurderingsresultat",
        revurderingsvedtaksdato = LocalDate.now(),
        begrunnelseForTilbakekreving = "Begrunnelse for tilbakekreving",
    )

fun varselbrev() =
    Varselbrev(
        internId = UUID.randomUUID(),
        opprettetDato = LocalDate.now(),
        varsletBeløp = 10000L,
    )
