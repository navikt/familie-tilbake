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
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
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
    behandlendeEnhet: String? = "0425",
) = FagsysteminfoHendelse(
    aktør = Aktør.Person(bruker().ident),
    revurdering = FagsysteminfoHendelse.Revurdering(
        behandlingId = UUID.randomUUID().toString(),
        årsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
        årsakTilFeilutbetaling = "",
        vedtaksdato = LocalDate.now(),
    ),
    utvidPerioder = utvidPerioder,
    behandlendeEnhet = behandlendeEnhet,
)

fun brukerinfoHendelse() = BrukerinfoHendelse(
    ident = bruker().ident,
    navn = "test bruker",
    fødselsdato = LocalDate.now(),
    kjønn = Kjønn.MANN,
    dødsdato = null,
    språkkode = bruker().språkkode,
)

fun eksternFagsakBehandling(
    utvidPerioder: List<EksternFagsakRevurdering.UtvidetPeriode> = emptyList(),
): EksternFagsakRevurdering {
    return EksternFagsakRevurdering.Revurdering(
        id = UUID.randomUUID(),
        eksternId = UUID.randomUUID().toString(),
        årsakTilFeilutbetaling = "",
        revurderingsårsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
        vedtaksdato = LocalDate.now(),
        utvidedePerioder = utvidPerioder,
    )
}

fun behandling(
    kravgrunnlag: KravgrunnlagHendelse = kravgrunnlag(),
    eksternFagsakBehandling: EksternFagsakRevurdering = eksternFagsakBehandling(),
): Behandling {
    val kravgrunnlagReferanse = HistorikkStub.fakeReferanse(kravgrunnlag)
    val eksternFagsakBehandlingReferanse = HistorikkStub.fakeReferanse(eksternFagsakBehandling)
    return Behandling.nyBehandling(
        id = UUID.randomUUID(),
        type = Behandlingstype.TILBAKEKREVING,
        enhet = Enhet("", ""),
        ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER,
        eksternFagsakRevurdering = eksternFagsakBehandlingReferanse,
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
        oppdaget = Faktasteg.Vurdering.Oppdaget.Vurdering(
            dato = LocalDate.now(),
            beskrivelse = "Hva som helst",
            av = Faktasteg.Vurdering.Oppdaget.Av.Nav,
            id = UUID.randomUUID(),
        ),
    )
}

fun foreldelseVurdering() = Foreldelsesteg.Vurdering.IkkeForeldet("")

fun godTro(
    beløpIBehold: NivåAvForståelse.GodTro.BeløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
) = NivåAvForståelse.GodTro(
    beløpIBehold = beløpIBehold,
    begrunnelse = "",
    begrunnelseForGodTro = "",
)

fun forårsaketAvNavBurdeForstått(
    aktsomhet: NivåAvForståelse.Aktsomhet = NivåAvForståelse.Aktsomhet.Uaktsomhet(
        kanUnnlates4XRettsgebyr = skalIkkeUnnlates(),
        begrunnelse = "",
    ),
) = NivåAvForståelse.BurdeForstått(
    aktsomhet = aktsomhet,
    begrunnelse = "",
)

fun forårsaketAvNavForstod(
    aktsomhet: NivåAvForståelse.Aktsomhet = NivåAvForståelse.Aktsomhet.Forsett(
        begrunnelse = "",
    ),
) = NivåAvForståelse.BurdeForstått(
    aktsomhet = aktsomhet,
    begrunnelse = "",
)

fun forårsaketAvBrukerUaktsomt(
    unnlates4xRettsgebyr: KanUnnlates4xRettsgebyr = skalIkkeUnnlates(),
) = Skyldgrad.Uaktsomt(
    begrunnelse = "",
    begrunnelseAktsomhet = "",
    kanUnnlates4XRettsgebyr = unnlates4xRettsgebyr,
    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
)

fun forårsaketAvBrukerGrovtUaktsomt(
    skalReduseres: ReduksjonSærligeGrunner.SkalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
) = Skyldgrad.GrovUaktsomhet(
    begrunnelse = "",
    begrunnelseAktsomhet = "",
    reduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = "",
        grunner = emptySet(),
        skalReduseres = skalReduseres,
    ),
    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
)

fun forårsaketAvBrukerMedForsett() = Skyldgrad.Forsett(
    begrunnelse = "",
    begrunnelseAktsomhet = "",
    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
)

fun skalIkkeUnnlates(
    skalReduseres: ReduksjonSærligeGrunner.SkalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
) = KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
    reduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = "",
        grunner = emptySet(),
        skalReduseres = skalReduseres,
    ),
)

fun unnlates() = KanUnnlates4xRettsgebyr.Unnlates

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
