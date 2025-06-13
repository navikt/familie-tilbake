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
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.klarTilVisning
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.entities.BehandlingEntity
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
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
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
    val fatteVedtakSteg: FatteVedtakSteg,
) : Historikk.HistorikkInnslag<UUID>, FrontendDto<BehandlingDto> {
    val faktastegDto: FrontendDto<FaktaFeilutbetalingDto> get() = faktasteg
    val foreldelsestegDto: FrontendDto<VurdertForeldelseDto> get() = foreldelsesteg
    val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto> get() = vilkårsvurderingsteg
    val fatteVedtakStegDto: FrontendDto<TotrinnsvurderingDto> get() = fatteVedtakSteg
    lateinit var brevmottakerSteg: BrevmottakerSteg

    fun harLikePerioder(): Boolean = vilkårsvurderingsteg.harLikePerioder()

    fun tilEntity(): BehandlingEntity {
        return BehandlingEntity(
            internId = internId.toString(),
            eksternId = eksternId.toString(),
            behandlingstype = behandlingstype.name,
            opprettet = opprettet.toString(),
            sistEndret = sistEndret.toString(),
            enhet = enhet?.tilEntity(),
            årsak = årsak.name,
            ansvarligSaksbehandlerEntity = ansvarligSaksbehandler.tilEntity(),
            eksternFagsakBehandlingRefEntity = eksternFagsakBehandling.entry.tilEntity(),
            kravgrunnlagHendelseRefEntity = kravgrunnlag.entry.tilEntity(),
            foreldelsestegEntity = foreldelsesteg.tilEntity(),
            faktastegEntity = faktasteg.tilEntity(),
            vilkårsvurderingstegEntity = vilkårsvurderingsteg.tilEntity(),
            foreslåVedtakStegEntity = foreslåVedtakSteg.tilEntity(),
            fatteVedtakStegEntity = fatteVedtakSteg.tilEntity(),
        )
    }

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

    fun trengerIverksettelse(behovObservatør: BehovObservatør) {
        val beregning = lagBeregning()
        val delperioder = beregning.beregn()
        behovObservatør.håndter(
            IverksettelseBehov(
                behandlingId = internId,
                kravgrunnlagId = kravgrunnlag.entry.kravgrunnlagId,
                delperioder = delperioder,
                ansvarligSaksbehandler = ansvarligSaksbehandler().ident,
            ),
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
        this.ansvarligSaksbehandler = behandler
        faktasteg.behandleFakta(vurdering)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Vilkårsvurderingsteg.Vurdering,
    ) {
        this.ansvarligSaksbehandler = behandler
        vilkårsvurderingsteg.vurder(periode, vurdering)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) {
        this.ansvarligSaksbehandler = behandler
        foreldelsesteg.vurderForeldelse(periode, vurdering)
    }

    internal fun håndter(
        behandler: Behandler,
        vurdering: ForeslåVedtakSteg.Vurdering,
    ) {
        this.ansvarligSaksbehandler = behandler
        foreslåVedtakSteg.håndter(vurdering)
    }

    internal fun håndter(
        tilbakekreving: Tilbakekreving,
        beslutter: Behandler,
        behandlingssteg: Behandlingssteg,
        vurdering: FatteVedtakSteg.Vurdering,
    ) {
        fatteVedtakSteg.håndter(beslutter, ansvarligSaksbehandler, behandlingssteg, vurdering)
        if (fatteVedtakSteg.erFullstending()) {
            tilbakekreving.byttTilstand(IverksettVedtak)
        }
    }

    internal fun håndter(
        behandler: Behandler,
        brevmottaker: RegistrertBrevmottaker,
    ) {
        this.ansvarligSaksbehandler = behandler
        brevmottakerSteg.håndter(brevmottaker)
    }

    internal fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
    ) {
        this.ansvarligSaksbehandler = behandler
        brevmottakerSteg.fjernManuellBrevmottaker(manuellBrevmottakerId)
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
            val fatteVedtakSteg = FatteVedtakSteg.opprett()
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
                fatteVedtakSteg = fatteVedtakSteg,
            )
        }

        fun fraEntity(
            behandlingEntity: BehandlingEntity,
            eksternFagsakBehandlingHistorikk: EksternFagsakBehandlingHistorikk,
            kravgrunnlagHistorikk: KravgrunnlagHistorikk,
        ): Behandling {
            val eksternFagsak = eksternFagsakBehandlingHistorikk.nåværende()
            val kravgrunnlag = kravgrunnlagHistorikk.nåværende()
            return Behandling(
                internId = UUID.fromString(behandlingEntity.internId),
                eksternId = UUID.fromString(behandlingEntity.eksternId),
                behandlingstype = Behandlingstype.valueOf(behandlingEntity.behandlingstype),
                opprettet = LocalDateTime.parse(behandlingEntity.opprettet),
                sistEndret = LocalDateTime.parse(behandlingEntity.sistEndret),
                enhet = behandlingEntity.enhet?.fraEntity(),
                årsak = Behandlingsårsakstype.valueOf(behandlingEntity.årsak),
                ansvarligSaksbehandler = behandlingEntity.ansvarligSaksbehandlerEntity.fraEntity(),
                eksternFagsakBehandling = eksternFagsak,
                kravgrunnlag = kravgrunnlag,
                foreldelsesteg = behandlingEntity.foreldelsestegEntity.fraEntity(kravgrunnlag),
                faktasteg = behandlingEntity.faktastegEntity.fraEntity(eksternFagsak, kravgrunnlag),
                vilkårsvurderingsteg = Vilkårsvurderingsteg.fraEntity(behandlingEntity.vilkårsvurderingstegEntity, kravgrunnlag),
                foreslåVedtakSteg = behandlingEntity.foreslåVedtakStegEntity.fraEntity(),
                fatteVedtakSteg = FatteVedtakSteg.fraEntity(behandlingEntity.fatteVedtakStegEntity),
            )
        }
    }
}
