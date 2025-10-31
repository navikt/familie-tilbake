package no.nav.tilbakekreving

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v2.BrukerDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingObservatørOppsamler
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.TilBehandling
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID

val ANSVARLIG_SAKSBEHANDLER = Behandler.Saksbehandler("Z999999")
val ANSVARLIG_BESLUTTER = Behandler.Saksbehandler("Z111111")

fun eksternFagsak(
    eksternId: String = "101010",
    ytelse: Ytelse = Ytelse.Barnetrygd,
) = OpprettTilbakekrevingHendelse.EksternFagsak(
    eksternId = eksternId,
    ytelse = ytelse,
)

fun bruker() = BrukerDto(
    ident = "31126900011",
    språkkode = Språkkode.NB,
)

fun opprettTilbakekrevingHendelse(
    eksternFagsak: OpprettTilbakekrevingHendelse.EksternFagsak = eksternFagsak(),
    opprettelsesvalg: Opprettelsesvalg = Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
) = OpprettTilbakekrevingHendelse(
    eksternFagsak = eksternFagsak,
    opprettelsesvalg = opprettelsesvalg,
)

fun kravgrunnlag(
    vedtakGjelder: Aktør = Aktør.Person(bruker().ident),
    utbetalesTil: Aktør = Aktør.Person(bruker().ident),
    perioder: List<KravgrunnlagHendelse.Periode> = listOf(kravgrunnlagPeriode()),
): KravgrunnlagHendelse {
    val kravgrunnlagHendelse = KravgrunnlagHendelse(
        id = UUID.randomUUID(),
        vedtakId = BigInteger(128, Random()),
        kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NY,
        fagsystemVedtaksdato = LocalDate.now(),
        vedtakGjelder = vedtakGjelder,
        utbetalesTil = utbetalesTil,
        skalBeregneRenter = false,
        ansvarligEnhet = "0425",
        kontrollfelt = UUID.randomUUID().toString(),
        referanse = UUID.randomUUID().toString(),
        kravgrunnlagId = UUID.randomUUID().toString(),
        perioder = perioder,
    )
    kravgrunnlagHendelse.valider(Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
    return kravgrunnlagHendelse
}

fun kravgrunnlagPeriode(
    periode: Datoperiode = 1.januar til 31.januar,
    ytelsesbeløp: List<KravgrunnlagHendelse.Periode.Beløp> = ytelsesbeløp(),
) =
    KravgrunnlagHendelse.Periode(
        id = UUID.randomUUID(),
        periode = periode,
        månedligSkattebeløp = BigDecimal("0.0"),
        beløp = ytelsesbeløp + feilutbetalteBeløp(),
    )

fun ytelsesbeløp(
    tilbakekrevesBeløp: BigDecimal = 2000.kroner,
    opprinneligBeløp: BigDecimal = 12000.kroner,
) =
    listOf(
        KravgrunnlagHendelse.Periode.Beløp(
            id = UUID.randomUUID(),
            klassekode = "",
            klassetype = "YTEL",
            opprinneligUtbetalingsbeløp = opprinneligBeløp,
            nyttBeløp = opprinneligBeløp - tilbakekrevesBeløp,
            tilbakekrevesBeløp = tilbakekrevesBeløp,
            skatteprosent = BigDecimal("0.0"),
        ),
    )

fun feilutbetalteBeløp() =
    listOf(
        KravgrunnlagHendelse.Periode.Beløp(
            id = UUID.randomUUID(),
            klassekode = "",
            klassetype = "FEIL",
            opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
            nyttBeløp = BigDecimal("10000.0"),
            tilbakekrevesBeløp = BigDecimal("2000.0"),
            skatteprosent = BigDecimal("0.0"),
        ),
    )

fun fagsysteminfoHendelse(
    utvidPerioder: List<FagsysteminfoHendelse.UtvidetPeriode>? = null,
) = FagsysteminfoHendelse(
    aktør = Aktør.Person(bruker().ident),
    revurdering = FagsysteminfoHendelse.Revurdering(
        behandlingId = UUID.randomUUID().toString(),
        årsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
        årsakTilFeilutbetaling = "",
        vedtaksdato = LocalDate.now(),
    ),
    utvidPerioder = utvidPerioder,
)

fun varselbrevHendelse(varselbrevId: UUID) = VarselbrevSendtHendelse(
    varselbrevId = varselbrevId,
    journalpostId = UUID.randomUUID().toString(),
    sendtTid = LocalDateTime.now()
)

fun brukerinfoHendelse() = BrukerinfoHendelse(
    ident = bruker().ident,
    navn = "test bruker",
    fødselsdato = LocalDate.now(),
    kjønn = Kjønn.MANN,
    dødsdato = null,
    språkkode = bruker().språkkode,
)

fun defaultBrevmottaker() = RegistrertBrevmottaker.DefaultMottaker(
    id = UUID.randomUUID(),
    navn = "test bruker",
    personIdent = bruker().ident,
)

fun eksternFagsakBehandling(): EksternFagsakRevurdering {
    return EksternFagsakRevurdering.Revurdering(
        id = UUID.randomUUID(),
        eksternId = UUID.randomUUID().toString(),
        årsakTilFeilutbetaling = "",
        revurderingsårsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
        vedtaksdato = LocalDate.now(),
        utvidedePerioder = emptyList(),
    )
}

fun behandling(
    kravgrunnlag: KravgrunnlagHendelse = kravgrunnlag(),
): Behandling {
    val kravgrunnlagReferanse = HistorikkStub.fakeReferanse(kravgrunnlag)
    val eksternFagsakBehandling = HistorikkStub.fakeReferanse(eksternFagsakBehandling())
    return Behandling.nyBehandling(
        id = UUID.randomUUID(),
        type = Behandlingstype.TILBAKEKREVING,
        enhet = Enhet("", ""),
        ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER,
        eksternFagsakRevurdering = eksternFagsakBehandling,
        kravgrunnlag = kravgrunnlagReferanse,
        brevHistorikk = BrevHistorikk(mutableListOf()),
        behandlingObservatør = BehandlingObservatørOppsamler(),
        tilstand = TilBehandling,
    )
}

fun faktastegVurdering(
    periode: Datoperiode = 1.januar til 31.januar,
    årsak: String = "Årsak",
    uttalelse: Faktasteg.Uttalelse = Faktasteg.Uttalelse.Nei,
): Faktasteg.Vurdering {
    return Faktasteg.Vurdering(
        perioder = listOf(
            Faktasteg.FaktaPeriode(
                id = UUID.randomUUID(),
                periode = periode,
                rettsligGrunnlag = Hendelsestype.ANNET,
                rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
            ),
        ),
        årsakTilFeilutbetaling = årsak,
        uttalelse = uttalelse,
    )
}

fun foreldelseVurdering() = Foreldelsesteg.Vurdering.IkkeForeldet("")

fun forårsaketAvBrukerGrovtUaktsomt() = Skyldgrad.GrovUaktsomhet(
    begrunnelse = "",
    begrunnelseAktsomhet = "",
    reduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = "",
        grunner = emptySet(),
        skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
    ),
    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
)

fun godkjenning() = listOf(
    Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
    Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
    Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
    Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
)

fun iverksettelse(): IverksettelseHendelse {
    return IverksettelseHendelse(
        iverksattVedtakId = UUID.randomUUID(),
        vedtakId = BigInteger.ZERO,
    )
}
