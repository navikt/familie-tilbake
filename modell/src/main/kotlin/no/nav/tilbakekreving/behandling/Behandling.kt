package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
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
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.Historikk
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Behandling internal constructor(
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
    private val fatteVedtakSteg: FatteVedtakSteg,
    private var påVent: PåVent?,
) : Historikk.HistorikkInnslag<UUID> {
    val faktastegDto: FrontendDto<FaktaFeilutbetalingDto> get() = faktasteg
    val foreldelsestegDto: FrontendDto<VurdertForeldelseDto> get() = foreldelsesteg
    val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto> get() = vilkårsvurderingsteg
    val fatteVedtakStegDto: FrontendDto<TotrinnsvurderingDto> get() = fatteVedtakSteg
    lateinit var brevmottakerSteg: BrevmottakerSteg

    fun harLikePerioder(): Boolean = vilkårsvurderingsteg.harLikePerioder()

    fun tilEntity(): BehandlingEntity {
        return BehandlingEntity(
            internId = internId,
            eksternId = eksternId,
            behandlingstype = behandlingstype,
            opprettet = opprettet,
            sistEndret = sistEndret,
            enhet = enhet?.tilEntity(),
            årsak = årsak,
            ansvarligSaksbehandler = ansvarligSaksbehandler.tilEntity(),
            eksternFagsakBehandlingRef = eksternFagsakBehandling.tilEntity(),
            kravgrunnlagRef = kravgrunnlag.tilEntity(),
            foreldelsestegEntity = foreldelsesteg.tilEntity(),
            faktastegEntity = faktasteg.tilEntity(),
            vilkårsvurderingstegEntity = vilkårsvurderingsteg.tilEntity(),
            foreslåVedtakStegEntity = foreslåVedtakSteg.tilEntity(),
            fatteVedtakStegEntity = fatteVedtakSteg.tilEntity(),
            påVentEntity = påVent?.tilEntity(),
        )
    }

    fun sporingsinformasjon(): Sporing {
        return Sporing(eksternFagsakBehandling.entry.eksternId, internId.toString())
    }

    private fun steg() = listOf(
        faktasteg,
        foreldelsesteg,
        vilkårsvurderingsteg,
        foreslåVedtakSteg,
        fatteVedtakSteg,
    )

    private fun behandlingsstatus() =
        steg().firstOrNull { !it.erFullstendig() }
            ?.behandlingsstatus
            ?: steg().last().behandlingsstatus

    private fun lagBeregning(): Beregning {
        return Beregning(
            beregnRenter = false,
            tilbakekrevLavtBeløp = true,
            vilkårsvurderingsteg,
            foreldelsesteg.perioder(),
            kravgrunnlag.entry,
            sporingsinformasjon(),
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

    fun trengerIverksettelse(
        behovObservatør: BehovObservatør,
        ytelsestype: Ytelsestype,
        aktør: Aktør,
    ) {
        val beregning = lagBeregning()
        val delperioder = beregning.beregn()
        behovObservatør.håndter(
            IverksettelseBehov(
                behandlingId = internId,
                kravgrunnlagId = kravgrunnlag.entry.kravgrunnlagId,
                delperioder = delperioder,
                ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
                ytelsestype = ytelsestype,
                aktør = aktør,
                behandlingstype = behandlingstype,
            ),
        )
    }

    private fun kanBesluttes(behandler: Behandler, kanBeslutte: Boolean): Boolean {
        return fatteVedtakSteg.erFullstendig() && behandler != ansvarligSaksbehandler && kanBeslutte
    }

    private fun kanEndres(behandler: Behandler, kanBeslutte: Boolean): Boolean {
        if (kanBesluttes(behandler, kanBeslutte)) return false
        return !foreslåVedtakSteg.erFullstendig() || behandler != ansvarligSaksbehandler && kanBeslutte
    }

    fun tilFrontendDto(behandler: Behandler, kanBeslutte: Boolean): BehandlingDto {
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
            resultatstype = when (hentVedtaksresultat()) {
                Vedtaksresultat.FULL_TILBAKEBETALING -> Behandlingsresultatstype.FULL_TILBAKEBETALING
                Vedtaksresultat.DELVIS_TILBAKEBETALING -> Behandlingsresultatstype.DELVIS_TILBAKEBETALING
                Vedtaksresultat.INGEN_TILBAKEBETALING -> Behandlingsresultatstype.INGEN_TILBAKEBETALING
                null -> null
            },
            ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter?.ident,
            erBehandlingPåVent = påVent != null,
            kanHenleggeBehandling = false,
            kanRevurderingOpprettes = true,
            harVerge = false,
            kanEndres = kanEndres(behandler, kanBeslutte),
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
            erNyModell = true,
        )
    }

    fun tilOppsummeringDto(): BehandlingsoppsummeringDto {
        return BehandlingsoppsummeringDto(
            behandlingId = internId,
            eksternBrukId = eksternId,
            type = behandlingstype,
            status = behandlingsstatus(),
        )
    }

    internal fun håndter(
        behandler: Behandler,
        vurdering: Faktasteg.Vurdering,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus(håndtertSteg = "fakta")
        faktasteg.vurder(vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Vilkårsvurderingsteg.Vurdering,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("vilkårsvurdering")
        vilkårsvurderingsteg.vurder(periode, vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("foreldelse")
        foreldelsesteg.vurderForeldelse(periode, vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        behandler: Behandler,
        vurdering: ForeslåVedtakSteg.Vurdering,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("vedtaksforslag")
        foreslåVedtakSteg.håndter(vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        beslutter: Behandler,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("behandlingsutfall")
        for ((behandlingssteg, vurdering) in vurderinger) {
            fatteVedtakSteg.håndter(beslutter, ansvarligSaksbehandler, behandlingssteg, vurdering, sporingsinformasjon())
        }
        oppdaterBehandler(ansvarligSaksbehandler)
    }

    internal fun håndter(
        behandler: Behandler,
        brevmottaker: RegistrertBrevmottaker,
        observatør: BehandlingObservatør,
    ) {
        brevmottakerSteg.håndter(brevmottaker, sporingsinformasjon())
        oppdaterBehandler(behandler)
    }

    internal fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
        observatør: BehandlingObservatør,
    ) {
        brevmottakerSteg.fjernManuellBrevmottaker(manuellBrevmottakerId, sporingsinformasjon())
        oppdaterBehandler(behandler)
    }

    internal fun opprettBrevmottaker(
        navn: String,
        ident: String,
    ) {
        brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
    }

    fun settPåVent(
        årsak: Venteårsak,
        utløpsdato: LocalDate,
        begrunnelse: String?,
    ) {
        påVent = PåVent(
            årsak = årsak,
            utløpsdato = utløpsdato,
            begrunnelse = begrunnelse,
        )
    }

    fun taAvVent() {
        påVent = null
    }

    private fun validerBehandlingstatus(håndtertSteg: String) {
        if (påVent != null) {
            throw ModellFeil.UgyldigOperasjonException(
                "Behandling er satt på vent. Kan ikke håndtere $håndtertSteg.",
                sporingsinformasjon(),
            )
        }
    }

    fun aktiverBrevmottakerSteg() = brevmottakerSteg.aktiverSteg()

    fun deaktiverBrevmottakerSteg() = brevmottakerSteg.deaktiverSteg()

    fun lagNullstiltBehandling(
        brevHistorikk: BrevHistorikk,
        behandlingObservatør: BehandlingObservatør,
    ): Behandling {
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
            behandlingObservatør = behandlingObservatør,
        )
    }

    fun kanUtbetales(): Boolean = fatteVedtakSteg.erFullstendig()

    fun hentBehandlingsinformasjon(): Behandlingsinformasjon {
        return Behandlingsinformasjon(
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            opprettetTid = opprettet,
            behandlingId = internId,
            enhet = enhet,
            behandlingstype = behandlingstype,
        )
    }

    fun oppdaterBehandler(ansvarligSaksbehandler: Behandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler
    }

    fun utførSideeffekt(observatør: BehandlingObservatør) {
        observatør.behandlingOppdatert(
            behandlingId = internId,
            eksternBehandlingId = eksternFagsakBehandling.entry.eksternId,
            vedtaksresultat = hentVedtaksresultat(),
            behandlingstatus = this.behandlingsstatus(),
            venterPåBruker = påVent?.avventerBruker() ?: false,
            ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter?.ident,
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder(),
            totalFeilutbetaltPeriode = kravgrunnlag.entry.perioder.minOf { it.periode.fom } til kravgrunnlag.entry.perioder.maxOf { it.periode.tom },
        )
    }

    fun hentVedtaksresultat(): Vedtaksresultat? {
        if (fatteVedtakSteg.erFullstendig()) {
            return lagBeregning().oppsummer().vedtaksresultat
        }
        return null
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
            behandlingObservatør: BehandlingObservatør,
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
                påVent = null,
            ).also {
                it.utførSideeffekt(behandlingObservatør)
            }
        }
    }
}
