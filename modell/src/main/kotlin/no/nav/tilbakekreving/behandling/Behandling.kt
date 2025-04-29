package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.api.v1.dto.BeregnetPeriodeDto
import no.nav.tilbakekreving.api.v1.dto.BeregnetPerioderDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.BrevmottakerSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.klarTilVisning
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.saksbehandler.Saksbehandling
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import java.time.LocalDateTime
import java.util.UUID

class Behandling private constructor(
    override val internId: UUID,
    private val eksternId: UUID,
    private val behandlingstype: Behandlingstype,
    private val opprettet: LocalDateTime,
    private val sistEndret: LocalDateTime,
    private val enhet: Enhet?,
    private val årsak: Behandlingsårsakstype,
    private var ansvarligSaksbehandler: Behandler,
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    val foreldelsesteg: Foreldelsesteg,
    private val faktasteg: Faktasteg,
    private val vilkårsvurderingsteg: Vilkårsvurderingsteg,
    private val foreslåVedtakSteg: ForeslåVedtakSteg,
) : Historikk.HistorikkInnslag<UUID>, FrontendDto<BehandlingDto>, Saksbehandling {
    private val fatteVedtakSteg = FatteVedtakSteg.opprett(this)

    val faktastegDto: FrontendDto<FaktaFeilutbetalingDto> get() = faktasteg
    val foreldelsestegDto: FrontendDto<VurdertForeldelseDto> get() = foreldelsesteg
    val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto> get() = vilkårsvurderingsteg
    val fatteVedtakStegDto: FrontendDto<TotrinnsvurderingDto> get() = fatteVedtakSteg
    lateinit var brevmottakerSteg: BrevmottakerSteg

    fun harLikePerioder(): Boolean = vilkårsvurderingsteg.harLikePerioder()

    private fun steg() = listOf(
        faktasteg,
        foreldelsesteg,
        vilkårsvurderingsteg,
        foreslåVedtakSteg,
        fatteVedtakSteg,
    )

    private fun behandlingsstatus() =
        steg().firstOrNull { !it.erFullstending() }
            ?.behandlingsstatus
            ?: Behandlingsstatus.AVSLUTTET

    fun beregnSplittetPeriode(
        perioder: List<Datoperiode>,
    ): BeregnetPerioderDto = BeregnetPerioderDto(perioder.map { BeregnetPeriodeDto(it, kravgrunnlag.entry.totaltBeløpFor(it)) })

    fun splittForeldetPerioder(perioder: List<Datoperiode>) {
        foreldelsesteg.splittPerioder(perioder)
        vilkårsvurderingsteg.splittPerioder(perioder)
    }

    fun splittVilkårsvurdertePerioder(perioder: List<Datoperiode>) {
        vilkårsvurderingsteg.splittPerioder(perioder)
    }

    private fun lagBeregning(): Beregning {
        return Beregning(
            beregnRenter = false,
            tilbakekrevLavtBeløp = true,
            vilkårsvurderingsteg,
            foreldelsesteg.perioder(),
            kravgrunnlag.entry,
        )
    }

    fun beregnForFrontend(): BeregningsresultatDto {
        val beregning = lagBeregning().oppsummer()
        return BeregningsresultatDto(
            beregning.beregningsresultatsperioder.map {
                BeregningsresultatsperiodeDto(
                    periode = it.periode,
                    vurdering = it.vurdering,
                    feilutbetaltBeløp = it.feilutbetaltBeløp,
                    andelAvBeløp = it.andelAvBeløp,
                    renteprosent = it.renteprosent,
                    tilbakekrevingsbeløp = it.tilbakekrevingsbeløp,
                    tilbakekrevesBeløpEtterSkatt = it.tilbakekrevingsbeløpEtterSkatt,
                )
            },
            beregning.vedtaksresultat,
            faktasteg.tilFrontendDto().vurderingAvBrukersUttalelse,
        )
    }

    override fun ansvarligSaksbehandler(): Behandler {
        return ansvarligSaksbehandler
    }

    override fun oppdaterAnsvarligSaksbehandler(behandler: Behandler) {
        ansvarligSaksbehandler = behandler
    }

    override fun tilFrontendDto(): BehandlingDto {
        return BehandlingDto(
            eksternBrukId = eksternId,
            behandlingId = internId,
            erBehandlingHenlagt = false,
            type = behandlingstype,
            status = behandlingsstatus(),
            opprettetDato = opprettet.toLocalDate(),
            avsluttetDato = null,
            endretTidspunkt = sistEndret,
            vedtaksdato = null,
            enhetskode = enhet?.kode ?: "Ukjent",
            enhetsnavn = enhet?.navn ?: "Ukjent",
            resultatstype = null,
            ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter?.ident,
            erBehandlingPåVent = false,
            kanHenleggeBehandling = false,
            kanRevurderingOpprettes = true,
            harVerge = false,
            kanEndres = behandlingsstatus() != Behandlingsstatus.AVSLUTTET,
            kanSetteTilbakeTilFakta = true,
            varselSendt = false,
            behandlingsstegsinfo = listOf(
                listOf(
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.GRUNNLAG,
                        Behandlingsstegstatus.AUTOUTFØRT,
                    ),
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.VARSEL,
                        Behandlingsstegstatus.AUTOUTFØRT,
                    ),
                ),
                steg().klarTilVisning().map {
                    BehandlingsstegsinfoDto(
                        it.type,
                        it.behandlingsstegstatus(),
                    )
                },
            ).flatten(),
            fagsystemsbehandlingId = eksternFagsakBehandling.entry.eksternId,
            // TODO
            eksternFagsakId = "TODO",
            behandlingsårsakstype = årsak,
            støtterManuelleBrevmottakere = true,
            harManuelleBrevmottakere = false,
            manuelleBrevmottakere = emptyList(),
            begrunnelseForTilbakekreving = eksternFagsakBehandling.entry.begrunnelseForTilbakekreving,
            saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
        )
    }

    internal fun håndter(
        behandler: Behandler,
        vurdering: FaktaFeilutbetalingsperiodeDto,
    ) {
        oppdaterAnsvarligSaksbehandler(behandler)
        faktasteg.behandleFakta(vurdering)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Vilkårsvurderingsteg.Vurdering,
    ) {
        oppdaterAnsvarligSaksbehandler(behandler)
        vilkårsvurderingsteg.vurder(periode, vurdering)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) {
        oppdaterAnsvarligSaksbehandler(behandler)
        foreldelsesteg.vurderForeldelse(periode, vurdering)
    }

    internal fun håndter(
        behandler: Behandler,
        vurdering: ForeslåVedtakSteg.Vurdering,
    ) {
        oppdaterAnsvarligSaksbehandler(behandler)
        foreslåVedtakSteg.håndter(vurdering)
    }

    internal fun håndter(
        tilbakekreving: Tilbakekreving,
        beslutter: Behandler,
        behandlingssteg: Behandlingssteg,
        vurdering: FatteVedtakSteg.Vurdering,
    ) {
        fatteVedtakSteg.håndter(beslutter, behandlingssteg, vurdering)
        if (fatteVedtakSteg.erFullstending()) {
            tilbakekreving.byttTilstand(IverksettVedtak)
        }
    }

    internal fun håndter(
        behandler: Behandler,
        brevmottaker: BrevmottakerSteg.RegistrertBrevmottaker,
    ) {
        if (brevmottakerSteg.erStegetAktivert()) {
            oppdaterAnsvarligSaksbehandler(behandler)
            brevmottakerSteg.håndter(brevmottaker)
        } else {
            throw Exception("BrevmottakerSteg er ikke aktivert.")
        }
    }

    internal fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
    ) {
        if (brevmottakerSteg.erStegetAktivert()) {
            oppdaterAnsvarligSaksbehandler(behandler)
            brevmottakerSteg.fjernManuelBrevmottaker(manuellBrevmottakerId)
        } else {
            throw Exception("BrevmottakerSteg er ikke aktivert.")
        }
    }

    internal fun opprettBrevmottaker(
        navn: String,
        ident: String,
    ) {
        brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
    }

    fun aktiverBrevmottakerSteg() = brevmottakerSteg.aktiverSteg()

    fun deaktiverBrevmottakerSteg() = brevmottakerSteg.deaktiverSteg()

    fun lagNullstiltBehandling(brevHistorikk: BrevHistorikk): Behandling {
        return nyBehandling(
            internId = UUID.randomUUID(),
            eksternId = eksternId,
            behandlingstype = Behandlingstype.REVURDERING_TILBAKEKREVING,
            opprettet = opprettet,
            enhet = enhet,
            årsak = årsak,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            sistEndret = LocalDateTime.now(),
            eksternFagsakBehandling = eksternFagsakBehandling,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = brevHistorikk,
        )
    }

    companion object {
        fun nyBehandling(
            internId: UUID,
            eksternId: UUID,
            behandlingstype: Behandlingstype,
            opprettet: LocalDateTime,
            sistEndret: LocalDateTime = opprettet,
            enhet: Enhet?,
            årsak: Behandlingsårsakstype,
            ansvarligSaksbehandler: Behandler,
            eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            brevHistorikk: BrevHistorikk,
        ): Behandling {
            val foreldelsesteg = Foreldelsesteg.opprett(kravgrunnlag)
            val faktasteg = Faktasteg.opprett(eksternFagsakBehandling, kravgrunnlag, brevHistorikk, LocalDateTime.now(), Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL)
            val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(kravgrunnlag, foreldelsesteg)
            val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()

            return Behandling(
                internId = internId,
                eksternId = eksternId,
                behandlingstype = behandlingstype,
                opprettet = opprettet,
                sistEndret = sistEndret,
                enhet = enhet,
                årsak = årsak,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                eksternFagsakBehandling = eksternFagsakBehandling,
                kravgrunnlag = kravgrunnlag,
                foreldelsesteg = foreldelsesteg,
                faktasteg = faktasteg,
                vilkårsvurderingsteg = vilkårsvurderingsteg,
                foreslåVedtakSteg = foreslåVedtakSteg,
            )
        }
    }
}
