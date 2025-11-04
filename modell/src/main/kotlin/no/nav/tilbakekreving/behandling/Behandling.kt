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
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.klarTilVisning
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
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
import no.nav.tilbakekreving.tilstand.Tilstand
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Behandling internal constructor(
    override val id: UUID,
    private val type: Behandlingstype,
    private val opprettet: LocalDateTime,
    private var sistEndret: LocalDateTime,
    private val enhet: Enhet?,
    private val revurderingsårsak: Behandlingsårsakstype?,
    private var ansvarligSaksbehandler: Behandler,
    private var eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    val foreldelsesteg: Foreldelsesteg,
    private val faktasteg: Faktasteg,
    private val vilkårsvurderingsteg: Vilkårsvurderingsteg,
    private val foreslåVedtakSteg: ForeslåVedtakSteg,
    private val fatteVedtakSteg: FatteVedtakSteg,
    private var påVent: PåVent?,
    var brevmottakerSteg: BrevmottakerSteg?,
) : Historikk.HistorikkInnslag<UUID> {
    fun faktastegFrontendDto(
        opprettelsesvalg: Opprettelsesvalg,
        tilbakekrevingOpprettet: LocalDateTime,
    ): FaktaFeilutbetalingDto {
        return faktasteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlag.entry,
            eksternFagsakRevurdering = eksternFagsakRevurdering.entry,
            opprettelsesvalg = opprettelsesvalg,
            tilbakekrevingOpprettet = tilbakekrevingOpprettet,
        )
    }

    val foreldelsestegDto: FrontendDto<VurdertForeldelseDto> get() = FrontendDto {
        foreldelsesteg.tilFrontendDto(kravgrunnlag.entry)
    }
    val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto> get() = FrontendDto {
        vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag.entry)
    }
    val fatteVedtakStegDto: FrontendDto<TotrinnsvurderingDto> get() = fatteVedtakSteg

    fun harLikePerioder(): Boolean = vilkårsvurderingsteg.harLikePerioder()

    fun tilEntity(tilbakekrevingId: String): BehandlingEntity {
        return BehandlingEntity(
            id = id,
            tilbakekrevingId = tilbakekrevingId,
            type = type,
            opprettet = opprettet,
            sistEndret = sistEndret,
            enhet = enhet?.tilEntity(),
            revurderingsårsak = revurderingsårsak,
            ansvarligSaksbehandler = ansvarligSaksbehandler.tilEntity(),
            eksternFagsakBehandlingRef = eksternFagsakRevurdering.tilEntity(),
            kravgrunnlagRef = kravgrunnlag.tilEntity(),
            foreldelsestegEntity = foreldelsesteg.tilEntity(id),
            faktastegEntity = faktasteg.tilEntity(id),
            vilkårsvurderingstegEntity = vilkårsvurderingsteg.tilEntity(id),
            foreslåVedtakStegEntity = foreslåVedtakSteg.tilEntity(id),
            fatteVedtakStegEntity = fatteVedtakSteg.tilEntity(id),
            påVentEntity = påVent?.tilEntity(id),
            brevmottakerStegEntity = brevmottakerSteg?.tilEntity(id),
        )
    }

    fun sporingsinformasjon(): Sporing {
        return Sporing(eksternFagsakRevurdering.entry.eksternId, id.toString())
    }

    internal fun steg(): List<Saksbehandlingsteg> = listOf(
        faktasteg,
        foreldelsesteg,
        vilkårsvurderingsteg,
        foreslåVedtakSteg,
        fatteVedtakSteg,
    )

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
            faktasteg.vurderingAvBrukersUttalelse(),
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
                behandlingId = id,
                kravgrunnlagId = kravgrunnlag.entry.kravgrunnlagId,
                delperioder = delperioder,
                ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
                ytelsestype = ytelsestype,
                aktør = aktør,
                behandlingstype = type,
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

    internal fun tilFrontendDto(tilstand: Tilstand, behandler: Behandler, kanBeslutte: Boolean): BehandlingDto {
        return BehandlingDto(
            eksternBrukId = id,
            behandlingId = id,
            erBehandlingHenlagt = false,
            type = type,
            status = tilstand.behandlingsstatus(this),
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
            fagsystemsbehandlingId = eksternFagsakRevurdering.entry.eksternId,
            // TODO
            eksternFagsakId = "TODO",
            behandlingsårsakstype = revurderingsårsak,
            støtterManuelleBrevmottakere = true,
            harManuelleBrevmottakere = false,
            manuelleBrevmottakere = emptyList(),
            begrunnelseForTilbakekreving = eksternFagsakRevurdering.entry.årsakTilFeilutbetaling,
            saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
            erNyModell = true,
        )
    }

    internal fun tilOppsummeringDto(tilstand: Tilstand): BehandlingsoppsummeringDto {
        return BehandlingsoppsummeringDto(
            behandlingId = id,
            eksternBrukId = id,
            type = type,
            status = tilstand.behandlingsstatus(this),
        )
    }

    internal fun håndter(
        behandler: Behandler,
        vurdering: Faktasteg.Vurdering,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus(håndtertSteg = "fakta", faktasteg)
        faktasteg.vurder(vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("vilkårsvurdering", vilkårsvurderingsteg)
        vilkårsvurderingsteg.vurder(periode, vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("foreldelse", foreldelsesteg)
        foreldelsesteg.vurderForeldelse(periode, vurdering)
        oppdaterBehandler(behandler)
    }

    internal fun håndterForeslåVedtak(
        behandler: Behandler,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("vedtaksforslag", foreslåVedtakSteg)
        foreslåVedtakSteg.håndter()
        oppdaterBehandler(behandler)
    }

    internal fun håndter(
        beslutter: Behandler,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
        observatør: BehandlingObservatør,
    ) {
        validerBehandlingstatus("behandlingsutfall", fatteVedtakSteg)
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
        brevmottakerSteg!!.håndter(brevmottaker, sporingsinformasjon())
        oppdaterBehandler(behandler)
    }

    internal fun oppdaterEksternFagsak(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
    ) {
        if (sistEndret == opprettet) {
            this.eksternFagsakRevurdering = eksternFagsakRevurdering
            flyttTilbakeTilFakta()
        }
    }

    internal fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
        observatør: BehandlingObservatør,
    ) {
        brevmottakerSteg!!.fjernManuellBrevmottaker(manuellBrevmottakerId, sporingsinformasjon())
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
            id = UUID.randomUUID(),
            årsak = årsak,
            utløpsdato = utløpsdato,
            begrunnelse = begrunnelse,
        )
    }

    fun taAvVent() {
        påVent = null
    }

    private fun validerBehandlingstatus(håndtertSteg: String, steg: Saksbehandlingsteg) {
        if (!steg().klarTilVisning().contains(steg)) {
            throw ModellFeil.UgyldigOperasjonException(
                "Behandlingen er i ${steg().klarTilVisning().last().type} og kan ikke behandle vurdering for ${steg.type}",
                sporingsinformasjon(),
            )
        }
        if (påVent != null) {
            throw ModellFeil.UgyldigOperasjonException(
                "Behandling er satt på vent. Kan ikke håndtere $håndtertSteg.",
                sporingsinformasjon(),
            )
        }
    }

    fun aktiverBrevmottakerSteg() = brevmottakerSteg!!.aktiverSteg()

    fun deaktiverBrevmottakerSteg() = brevmottakerSteg!!.deaktiverSteg()

    fun kanUtbetales(): Boolean = fatteVedtakSteg.erFullstendig()

    fun hentBehandlingsinformasjon(): Behandlingsinformasjon {
        return Behandlingsinformasjon(
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            opprettetTid = opprettet,
            behandlingId = id,
            enhet = enhet,
            behandlingstype = type,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
        )
    }

    fun hentForhåndsvarselinfo(): Forhåndsvarselinfo = Forhåndsvarselinfo(
        behandlendeEnhetNavn = enhet?.navn ?: "Ukjent", // Todo Fjern ukjent når enhet er på plass
        ansvarligSaksbehandler = ansvarligSaksbehandler,
        beløp = totaltFeilutbetaltBeløp().toLong(),
        feilutbetaltePerioder = kravgrunnlag.entry.perioder.map {
            it.periode
        },
        revurderingsvedtaksdato = eksternFagsakRevurdering.entry.vedtaksdato,
    )

    fun oppdaterBehandler(ansvarligSaksbehandler: Behandler) {
        this.sistEndret = LocalDateTime.now()
        this.ansvarligSaksbehandler = ansvarligSaksbehandler
    }

    internal fun utførSideeffekt(tilstand: Tilstand, observatør: BehandlingObservatør) {
        observatør.behandlingOppdatert(
            behandlingId = id,
            eksternBehandlingId = eksternFagsakRevurdering.entry.eksternId,
            vedtaksresultat = hentVedtaksresultat(),
            behandlingstatus = tilstand.behandlingsstatus(this),
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

    fun totaltFeilutbetaltBeløp(): BigDecimal {
        return kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder()
    }

    fun feilutbetaltePerioder(): List<Datoperiode> {
        return kravgrunnlag.entry.perioder.map {
            it.periode
        }
    }

    fun fullstendigPeriode(): Datoperiode {
        val perioder = kravgrunnlag.entry.perioder.map { it.periode }
            .map { eksternFagsakRevurdering.entry.utvidPeriode(it) }
        return perioder.minOf { it.fom } til perioder.maxOf { it.tom }
    }

    fun flyttTilbakeTilFakta() = steg().forEach { it.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry) }

    fun trekkTilbakeFraGodkjenning() = foreslåVedtakSteg.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)

    fun sendVedtakIverksatt(
        forrigeBehandlingId: UUID?,
        eksternFagsystemId: String,
        ytelse: Ytelse,
        endringObservatør: EndringObservatør,
    ) {
        val beregning = lagBeregning()
        endringObservatør.vedtakFattet(
            behandlingId = id,
            forrigeBehandlingId = forrigeBehandlingId,
            behandlingOpprettet = OffsetDateTime.of(opprettet, ZoneOffset.UTC),
            eksternFagsystemId = eksternFagsystemId,
            eksternBehandlingId = eksternFagsakRevurdering.entry.eksternId,
            ytelse = ytelse,
            vedtakFattetTidspunkt = OffsetDateTime.now(ZoneOffset.UTC),
            ansvarligEnhet = null,
            ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter!!.ident,
            vurderteUtbetalinger = beregning.beregn().map {
                val utvidetPeriode = eksternFagsakRevurdering.entry.utvidPeriode(it.periode)
                VurdertUtbetaling(
                    periode = utvidetPeriode,
                    rettsligGrunnlag = "Annet",
                    vilkårsvurdering = vilkårsvurderingsteg.oppsummer(utvidetPeriode),
                    beregning = VurdertUtbetaling.Beregning(
                        feilutbetaltBeløp = it.feilutbetaltBeløp(),
                        tilbakekrevesBeløp = it.tilbakekrevesBruttoMedRenter(),
                        rentebeløp = it.renter(),
                    ),
                )
            },
        )
    }

    companion object {
        internal fun nyBehandling(
            id: UUID,
            type: Behandlingstype,
            enhet: Enhet?,
            ansvarligSaksbehandler: Behandler,
            eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            brevHistorikk: BrevHistorikk,
            behandlingObservatør: BehandlingObservatør,
            tilstand: Tilstand,
        ): Behandling {
            val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry)
            val faktasteg = Faktasteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry, brevHistorikk)
            val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry, foreldelsesteg)
            val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()
            val fatteVedtakSteg = FatteVedtakSteg.opprett()
            val opprettet = LocalDateTime.now()
            return Behandling(
                id = id,
                type = type,
                opprettet = opprettet,
                sistEndret = opprettet,
                enhet = enhet,
                revurderingsårsak = null,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                eksternFagsakRevurdering = eksternFagsakRevurdering,
                kravgrunnlag = kravgrunnlag,
                foreldelsesteg = foreldelsesteg,
                faktasteg = faktasteg,
                vilkårsvurderingsteg = vilkårsvurderingsteg,
                foreslåVedtakSteg = foreslåVedtakSteg,
                fatteVedtakSteg = fatteVedtakSteg,
                påVent = null,
                brevmottakerSteg = null,
            ).also {
                it.utførSideeffekt(tilstand, behandlingObservatør)
            }
        }
    }
}
