package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.LesContext
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.aktør.Bruker
import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.klarTilVisning
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.VarselbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.Signatur
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
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
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdagetDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SammenslaaingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tekst.slåSammen
import no.nav.tilbakekreving.tilstand.Tilstand
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto as FrontendBeregningsresultatDto

class Behandling internal constructor(
    override val id: UUID,
    private val type: Behandlingstype,
    private val opprettet: LocalDateTime,
    private var sistEndret: LocalDateTime,
    private var enhet: Enhet?,
    private val revurderingsårsak: Behandlingsårsakstype?,
    private var ansvarligSaksbehandler: Behandler,
    private var eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
    private var kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    internal val foreldelsesteg: Foreldelsesteg,
    private val faktasteg: Faktasteg,
    private val vilkårsvurderingsteg: Vilkårsvurderingsteg,
    private val foreslåVedtakSteg: ForeslåVedtakSteg,
    private val fatteVedtakSteg: FatteVedtakSteg,
    private val forhåndsvarsel: Forhåndsvarsel,
    private var forrigeBehandlingsstatus: BehandlingsstatusModell,
) : Historikk.HistorikkInnslag<UUID>, BehandlingSkriveoperasjoner {
    internal fun nyFaktastegFrontendDto(varselbrev: Varselbrev?, klokke: Klokke): FaktaOmFeilutbetalingDto =
        faktasteg.nyTilFrontendDto(kravgrunnlag.entry, eksternFagsakRevurdering.entry, varselbrev, klokke)

    private fun bigqueryData(behandlingsstatus: BehandlingsstatusModell, ytelse: String, klokke: Klokke): BigQueryBehandlingDataDto {
        return BigQueryBehandlingDataDto(
            behandlingId = id.toString(),
            opprettetDato = opprettet,
            periode = fullstendigPeriode(),
            behandlingstype = type.name,
            ytelse = ytelse,
            beløp = totaltFeilutbetaltBeløp().toLong(),
            enhetNavn = enhet?.navn,
            enhetKode = enhet?.kode,
            status = behandlingsstatus.gammelFrontendDTO.name,
            resultat = hentVedtaksresultat(klokke)?.name,
        )
    }

    override fun nullstillForhåndsvarselUnntakOgUttalelse() = forhåndsvarsel.nullstillUnntakOgUttalelse()

    internal fun oppdaterKravgrunnlag(oppdatertKravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>, klokke: Klokke) {
        if (!faktasteg.erKlar(klokke)) {
            kravgrunnlag = oppdatertKravgrunnlag
        } else {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttetEtterBehandlingenErPåbegynt, sporingsinformasjon())
        }
    }

    override fun faktastegFrontendDto(
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

    override val foreldelsestegDto: FrontendDto<VurdertForeldelseDto> = FrontendDto { foreldelsesteg.tilFrontendDto(kravgrunnlag.entry) }

    override val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto> = FrontendDto { lesContext ->
        vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag.entry, eksternFagsakRevurdering.entry, foreldelsesteg, lesContext.klokke)
    }

    override val fatteVedtakStegDto: FrontendDto<TotrinnsvurderingDto> get() = fatteVedtakSteg

    internal fun tilEntity(tilbakekrevingId: String): BehandlingEntity {
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
            forhåndsvarselEntity = forhåndsvarsel.tilEntity(id),
            forrigeBehandlingsstatus = forrigeBehandlingsstatus,
        )
    }

    private fun sporingsinformasjon(): Sporing {
        return Sporing(eksternFagsakRevurdering.entry.eksternId, id.toString())
    }

    private fun steg(): List<Saksbehandlingsteg> = listOf(
        faktasteg,
        forhåndsvarsel,
        foreldelsesteg,
        vilkårsvurderingsteg,
        foreslåVedtakSteg,
        fatteVedtakSteg,
    )

    private fun lagBeregning(): Beregning {
        return Beregning(
            beregnRenter = true,
            tilbakekrevLavtBeløp = true,
            vilkårsvurderingsteg,
            foreldelsesteg.perioder(),
            kravgrunnlag.entry,
            sporingsinformasjon(),
        )
    }

    override fun beregnForFrontend(): BeregningsresultatDto {
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

    override fun hentVedtaksresultatForFrontend(): FrontendBeregningsresultatDto {
        return lagBeregning().oppsummer().tilFrontendDto()
    }

    internal fun trengerVarselbrevJournalføring(
        sideeffektContext: SideeffektContext,
        eksternFagsak: EksternFagsak,
        brukerinfo: Brukerinfo,
        varselbrevInfo: VarselbrevInfo,
    ) {
        sideeffektContext.behovObservatør.håndter(
            VarselbrevJournalføringBehov(
                brukerinfo = brukerinfo,
                behandlingId = id,
                info = varselbrevInfo,
                ytelse = eksternFagsak.ytelse,
                gjelderDødsfall = brukerinfo.dødsdato != null,
            ),
        )
    }

    internal fun trengerIverksettelse(
        sideeffektContext: SideeffektContext,
        ytelse: Ytelse,
        aktør: Aktør,
    ) {
        val beregning = lagBeregning()
        val delperioder = beregning.beregn()
        sideeffektContext.behovObservatør.håndter(
            IverksettelseBehov(
                behandlingId = id,
                kravgrunnlagId = kravgrunnlag.entry.kravgrunnlagId,
                delperioder = delperioder,
                ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
                ytelse = ytelse,
                aktør = aktør,
                behandlingstype = type,
            ),
        )
    }

    internal fun trengerVedtaksbrevJournalføring(
        sideeffektContext: SideeffektContext,
        brevId: UUID,
        ytelse: Ytelse,
        bruker: Bruker,
        fagsakId: String,
        tilbakekrevingId: String,
    ) {
        sideeffektContext.behovObservatør.håndter(
            VedtaksbrevJournalføringBehov(
                brevId = brevId,
                behandlingId = id,
                ytelse = ytelse,
                bruker = bruker.hentBrukerinfo(),
                fagsakId = fagsakId,
                journalførendeEnhet = enhet!!.kode,
                vedtaksbrevInfo = hentVedtaksbrevInfo(bruker, ytelse, tilbakekrevingId),
                vedtaksresultat = hentVedtaksresultat(sideeffektContext.klokke)!!,
                beslutter = fatteVedtakSteg.ansvarligBeslutter!!,
            ),
        )
    }

    internal fun lagVarselbrevDistribusjonBehov(
        journalpostId: String,
        ytelse: Ytelse,
        brevId: UUID,
        fagsakId: String,
        dokumentInfoId: String,
    ) = VarselbrevDistribusjonBehov(
        behandlingId = id,
        journalpostId = journalpostId,
        fagsakId = fagsakId,
        ytelse = ytelse,
        brevId = brevId,
        behandlerIdent = ansvarligSaksbehandler.ident,
        dokumentInfoId = dokumentInfoId,
    )

    internal fun lagVedtaksbrevDistribusjonBehov(
        journalpostId: String,
        dokumentInfoId: String,
        brevId: UUID,
        fagsystem: FagsystemDTO,
        fagsakId: String,
    ) = VedtaksbrevDistribusjonBehov(
        behandlingId = id,
        brevId = brevId,
        journalpostId = journalpostId,
        fagsystem = fagsystem,
        fagsakId = fagsakId,
        dokumentInfoId = dokumentInfoId,
    )

    internal fun venter(klokke: Klokke): Venter? = steg().firstNotNullOfOrNull { it.venter(klokke) }

    private fun skalBesluttes(klokke: Klokke): Boolean {
        return steg()
            .takeWhile { it.type != Behandlingssteg.FATTE_VEDTAK }
            .all { it.erFullstendig(klokke) }
    }

    internal fun førsteUfullstendigeSteg(klokke: Klokke) = steg().firstOrNull { !it.erKlar(klokke) }

    private fun kanEndres(behandler: Behandler, saksbehandlerKanBeslutte: Boolean, klokke: Klokke): Boolean {
        return when {
            skalBesluttes(klokke) -> behandler != ansvarligSaksbehandler && saksbehandlerKanBeslutte
            else -> true
        }
    }

    internal fun tilFrontendDto(tilstand: Tilstand, lesContext: LesContext, kanBeslutte: Boolean, behandlerRolle: BehandlerRolle): BehandlingDto {
        return BehandlingDto(
            eksternBrukId = id,
            behandlingId = id,
            erBehandlingHenlagt = false,
            type = type,
            status = tilstand.behandlingsstatus(this, lesContext.klokke).gammelFrontendDTO,
            opprettetDato = opprettet.toLocalDate(),
            avsluttetDato = null,
            endretTidspunkt = sistEndret,
            vedtaksdato = null,
            enhetskode = enhet?.kode ?: "Ukjent",
            enhetsnavn = enhet?.navn ?: "Ukjent",
            resultatstype = when (hentVedtaksresultat(lesContext.klokke)) {
                Vedtaksresultat.FULL_TILBAKEBETALING -> Behandlingsresultatstype.FULL_TILBAKEBETALING
                Vedtaksresultat.DELVIS_TILBAKEBETALING -> Behandlingsresultatstype.DELVIS_TILBAKEBETALING
                Vedtaksresultat.INGEN_TILBAKEBETALING -> Behandlingsresultatstype.INGEN_TILBAKEBETALING
                null -> null
            },
            ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter?.ident,
            erBehandlingPåVent = false,
            kanHenleggeBehandling = false,
            kanRevurderingOpprettes = true,
            harVerge = false,
            kanEndres = tilstand.kanEndresAvSaksbehandler && kanEndres(lesContext.behandler, kanBeslutte, lesContext.klokke),
            kanSetteTilbakeTilFakta = true,
            varselSendt = false,
            behandlingsstegsinfo = listOf(
                listOf(
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.GRUNNLAG,
                        Behandlingsstegstatus.AUTOUTFØRT,
                    ),
                ),
                steg().klarTilVisning(lesContext.klokke).map {
                    BehandlingsstegsinfoDto(
                        it.type,
                        it.behandlingsstegstatus(steg().klarTilVisning(lesContext.klokke), lesContext.klokke),
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
            innloggetRolle = utledInnloggetBrukerRolle(lesContext, behandlerRolle),
        )
    }

    private fun utledInnloggetBrukerRolle(lesContext: LesContext, behandlerRolle: BehandlerRolle): BehandlerRolle {
        return when {
            ansvarligSaksbehandler == lesContext.behandler -> BehandlerRolle.SAKSBEHANDLER
            behandlerRolle == BehandlerRolle.BESLUTTER && !skalBesluttes(lesContext.klokke) -> BehandlerRolle.SAKSBEHANDLER
            else -> behandlerRolle
        }
    }

    internal fun tilOppsummeringDto(tilstand: Tilstand, klokke: Klokke): BehandlingsoppsummeringDto {
        return BehandlingsoppsummeringDto(
            behandlingId = id,
            eksternBrukId = id,
            type = type,
            status = tilstand.behandlingsstatus(this, klokke).gammelFrontendDTO,
        )
    }

    fun hentVilkårsvurderingsperioder(): List<PeriodeInfoDto> {
        return vilkårsvurderingsteg.hentVilkårsvurderingsperioder()
    }

    internal fun oppdaterEksternFagsak(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        sideeffektContext: SideeffektContext,
    ) {
        if (sistEndret == opprettet) {
            this.eksternFagsakRevurdering = eksternFagsakRevurdering
            medSaksbehandling(sideeffektContext) { flyttTilbakeTilFakta() }
        }
    }

    internal fun oppdaterBehandlendeEnhet(enhetKode: String) {
        enhet = Enhet.forKode(enhetKode)
    }

    private fun validerBehandlingstatus(steg: Saksbehandlingsteg, klokke: Klokke) {
        if (!steg().klarTilVisning(klokke).contains(steg)) {
            throw ModellFeil.UgyldigOperasjonException(
                "Behandlingen er i ${steg().klarTilVisning(klokke).last().type} og kan ikke behandle vurdering for ${steg.type}",
                sporingsinformasjon(),
            )
        }
    }

    internal fun kanUtbetales(klokke: Klokke): Boolean = fatteVedtakSteg.erFullstendig(klokke) && !fatteVedtakSteg.erVedtakUnderkjent()

    internal fun hentBehandlingsinformasjon(): Behandlingsinformasjon {
        return Behandlingsinformasjon(
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            opprettetTid = opprettet,
            behandlingId = id,
            enhet = enhet,
            behandlingstype = type,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
        )
    }

    internal fun hentForhåndsvarselinfo(): Forhåndsvarselinfo = Forhåndsvarselinfo(
        behandlendeEnhet = enhet,
        ansvarligSaksbehandler = ansvarligSaksbehandler,
        beløp = totaltFeilutbetaltBeløp().toLong(),
        feilutbetaltePerioder = listOf(fullstendigPeriode()),
        revurderingsvedtaksdato = eksternFagsakRevurdering.entry.vedtaksdato,
    )

    internal fun forhåndsvarselFrontendDto(varselbrev: Varselbrev?) = forhåndsvarsel.tilFrontendDto(varselbrev)

    internal fun vurdertePerioderForBrev(): List<BegrunnetPeriode> {
        return vilkårsvurderingsteg.vurdertePerioderForBrev(steg().flatMap { it.meldingerTilSaksbehandler() }.toSet())
    }

    internal fun brevSignatur(): Signatur = Signatur(
        ansvarligSaksbehandlerIdent = ansvarligSaksbehandler.ident,
        ansvarligBeslutterIdent = fatteVedtakSteg.ansvarligBeslutter?.ident,
        ansvarligEnhet = enhet!!.navn,
    )

    private fun oppdaterBehandler(sideeffektContext: SideeffektContext) {
        this.sistEndret = sideeffektContext.klokke.nå()
        this.ansvarligSaksbehandler = sideeffektContext.behandler
    }

    internal fun <T> utførEndring(
        tilstand: () -> Tilstand,
        sideeffektContext: SideeffektContext,
        observatør: BehandlingObservatør,
        ytelse: Ytelse,
        callback: Behandling.() -> T,
    ): T {
        val statusFør = tilstand().behandlingsstatus(this, sideeffektContext.klokke)
        val result = callback()
        oppdaterAutomatiskeBehandlinger(sideeffektContext)
        val statusEtter = tilstand().behandlingsstatus(this, sideeffektContext.klokke)
        sendBehandlingsstatus(tilstand(), sideeffektContext, observatør)
        sideeffektContext.bigQueryService.oppdaterBehandling(bigqueryData(statusEtter, ytelse.hentYtelsesnavn(Språkkode.NB), sideeffektContext.klokke))
        if (statusFør != statusEtter || statusFør != forrigeBehandlingsstatus) {
            forrigeBehandlingsstatus = statusEtter
        }
        return result
    }

    private fun oppdaterAutomatiskeBehandlinger(sideeffektContext: SideeffektContext) {
        if (!foreslåVedtakSteg.erFullstendig(sideeffektContext.klokke)) {
            steg().forEach {
                it.automatiskVurder(
                    kravgrunnlag = kravgrunnlag.entry,
                    klokke = sideeffektContext.klokke,
                    behandlingslogg = sideeffektContext.behandlingslogg,
                    behandlingId = id,
                )
            }
        }
    }

    internal fun håndterPåminnelse(tilstand: Tilstand, sideeffektContext: SideeffektContext, observatør: BehandlingObservatør) {
        oppdaterAutomatiskeBehandlinger(sideeffektContext)
        sendBehandlingsstatus(tilstand, sideeffektContext, observatør)
    }

    private fun sendBehandlingsstatus(
        tilstand: Tilstand,
        sideeffektContext: SideeffektContext,
        observatør: BehandlingObservatør,
    ) {
        observatør.behandlingOppdatert(
            sideeffektContext,
            behandlingId = id,
            eksternBehandlingId = eksternFagsakRevurdering.entry.eksternId,
            vedtaksresultat = hentVedtaksresultat(sideeffektContext.klokke),
            behandlingsstatus = tilstand.behandlingsstatus(this, sideeffektContext.klokke),
            forrigeBehandlingsstatus = forrigeBehandlingsstatus,
            venter = venter(sideeffektContext.klokke),
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter?.ident,
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder(),
            totalFeilutbetaltPeriode = fullstendigPeriode(),
            ansvarligEnhet = enhet?.kode,
        )
    }

    private fun hentVedtaksresultat(klokke: Klokke): Vedtaksresultat? {
        if (fatteVedtakSteg.erFullstendig(klokke)) {
            return lagBeregning().oppsummer().vedtaksresultat
        }
        return null
    }

    private fun totaltFeilutbetaltBeløp(): BigDecimal {
        return kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder()
    }

    private fun fullstendigPeriode(): Datoperiode {
        val kravgrunnlagPerioder = kravgrunnlag.entry.datoperioder(eksternFagsakRevurdering.entry)
        return kravgrunnlagPerioder.minOf { it.fom } til kravgrunnlagPerioder.maxOf { it.tom }
    }

    inner class Saksbehandling internal constructor(
        private val context: SideeffektContext,
    ) {
        fun vurderFakta(vurdering: Faktasteg.Vurdering) {
            validerBehandlingstatus(faktasteg, context.klokke)
            faktasteg.vurder(vurdering)
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.FAKTA_VURDERT)
        }

        fun oppdaterFakta(
            oppdaget: OppdagetDto?,
            årsak: String?,
            perioder: List<OppdaterFaktaPeriodeDto>?,
        ) {
            validerBehandlingstatus(faktasteg, context.klokke)
            if (oppdaget != null) {
                faktasteg.vurder(oppdaget)
            }
            if (årsak != null) {
                faktasteg.vurder(årsak)
            }
            if (perioder != null) {
                faktasteg.vurder(perioder)
            }
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.FAKTA_VURDERT)
        }

        fun vurderVilkår(
            periode: Datoperiode,
            vurdering: ForårsaketAvBruker,
        ) {
            validerBehandlingstatus(vilkårsvurderingsteg, context.klokke)
            vilkårsvurderingsteg.vurder(periode, vurdering)
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.VILKÅRSVURDERING_VURDERT)
        }

        fun splittVilkårsvurdering(vilkårsvurderingId: UUID) {
            vilkårsvurderingsteg.splittVilkårsvurdering(vilkårsvurderingId)
        }

        fun slåSammenPerioder(sammenslaaingDto: SammenslaaingDto, sporing: Sporing) =
            vilkårsvurderingsteg.kopierVurderingerForSammenslåing(
                sammenslaaingDto = sammenslaaingDto,
                sporing,
            )

        fun vurderForeldelse(
            periode: Datoperiode,
            vurdering: Foreldelsesteg.Vurdering,
        ) {
            validerBehandlingstatus(foreldelsesteg, context.klokke)
            foreldelsesteg.vurderForeldelse(periode, vurdering)
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.FORELDELSE_VURDERT)
        }

        fun foreslåVedtak() {
            val tidligereManglendeSteg = steg()
                .takeWhile { it.type != Behandlingssteg.FORESLÅ_VEDTAK }
                .filter { !it.erKlar(context.klokke) }
            if (tidligereManglendeSteg.isNotEmpty()) {
                val stegtyper = tidligereManglendeSteg
                    .map { it.type }
                    .map { it.kortNavn }
                    .slåSammen()
                throw ModellFeil.UgyldigOperasjonException(
                    "Du må gjøre en ny vurdering av $stegtyper før du kan sende vedtaket til godkjenning hos beslutter",
                    sporing = sporingsinformasjon(),
                )
            }
            fatteVedtakSteg.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)
            validerBehandlingstatus(foreslåVedtakSteg, context.klokke)
            foreslåVedtakSteg.håndter()
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.FORESLÅ_VEDTAK_VURDERT)
        }

        fun fatteVedtak(vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>) {
            validerBehandlingstatus(fatteVedtakSteg, context.klokke)
            for ((behandlingssteg, vurdering) in vurderinger) {
                fatteVedtakSteg.håndter(context.behandler, ansvarligSaksbehandler, behandlingssteg, vurdering, sporingsinformasjon())
                if (vurdering is FatteVedtakSteg.Vurdering.Underkjent) {
                    steg()
                        .filter { it.type == behandlingssteg }
                        .forEach { it.underkjennSteget() }
                }
            }
            if (kanUtbetales(context.klokke)) {
                context.logg(
                    behandlingsloggstype = Behandlingsloggstype.VEDTAK_FATTET,
                    rolle = Rolle.BESLUTTER,
                )
            } else if (fatteVedtakSteg.erVedtakUnderkjent()) {
                foreslåVedtakSteg.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)

                context.logg(
                    behandlingsloggstype = Behandlingsloggstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER,
                    rolle = Rolle.BESLUTTER,
                )
            }
        }

        fun lagreUttalelse(
            uttalelseVurdering: UttalelseVurdering,
            uttalelseInfo: UttalelseInfo?,
            kommentar: String?,
        ) {
            forhåndsvarsel.lagreUttalelse(
                uttalelseVurdering = uttalelseVurdering,
                uttalelseInfo = uttalelseInfo,
                kommentar = kommentar,
            )

            context.logg(Behandlingsloggstype.BRUKER_UTTALELSE)
        }

        fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String): UttalelsesfristDto {
            val uttalelsesfrist = forhåndsvarsel.lagreFristUtsettelse(
                nyFrist = nyFrist,
                begrunnelse = begrunnelse,
            )

            context.logg(
                behandlingsloggstype = Behandlingsloggstype.UTSETT_UTTALELSESFRIST,
                EkstraInfo.BEGRUNNELSE_FOR_UTSATT_FRIST to begrunnelse,
                EkstraInfo.NY_FRIST_FOR_UTTALELSE to nyFrist,
            )
            return uttalelsesfrist
        }

        fun lagreForhåndsvarselUnntak(
            begrunnelseForUnntak: BegrunnelseForUnntak,
            beskrivelse: String,
        ) {
            forhåndsvarsel.lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = begrunnelseForUnntak,
                beskrivelse = beskrivelse,
            )

            context.logg(Behandlingsloggstype.UNNTAK_FOR_UTTALELSE)
        }

        fun flyttTilbakeTilFakta() {
            steg().forEach {
                it.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)
            }
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.BEHANDLING_NULLSTILLT)
        }

        fun trekkTilbakeFraGodkjenning() {
            foreslåVedtakSteg.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)
            oppdaterBehandler(context)

            context.logg(Behandlingsloggstype.TREKK_TILBAKE_FRA_GODKJENNING)
        }
    }

    internal fun <T> medSaksbehandling(context: SideeffektContext, block: Saksbehandling.() -> T): T =
        Saksbehandling(context).block()

    internal fun sendVedtakIverksatt(
        forrigeBehandlingId: UUID?,
        eksternFagsystemId: String,
        ytelse: Ytelse,
        endringObservatør: EndringObservatør,
        ansvarligEnhet: String?,
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
            ansvarligEnhet = ansvarligEnhet,
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

    internal fun opprettVarselbrev(
        varseltekstFraSaksbehandler: String,
        lesContext: LesContext,
    ): Varselbrev {
        val varselbrev = Varselbrev.opprett(
            ansvarligSaksbehandlerIdent = ansvarligSaksbehandler.ident,
            kravgrunnlag = kravgrunnlag,
            varseltekstFraSaksbehandler = varseltekstFraSaksbehandler,
            features = lesContext.features,
            klokke = lesContext.klokke,
        )
        return varselbrev
    }

    internal fun lagreOpprinneligFrist(fristForUttalelse: LocalDate) {
        forhåndsvarsel.lagreOpprinneligFrist(fristForUttalelse)
    }

    internal fun hentVedtaksbrevInfo(bruker: Bruker, ytelse: Ytelse, tilbakekrevingId: String): VedtaksbrevInfo {
        val resultat = lagBeregning().oppsummer()
        return VedtaksbrevInfo(
            brukerdata = bruker.brevmeta(),
            ytelse = ytelse.brevmeta(),
            signatur = brevSignatur(),
            perioder = vurdertePerioderForBrev(),
            bunntekster = Bunntekst.finnTekster(lagBeregning().oppsummer(), ytelse),
            skalTilbakekreves = resultat.vedtaksresultat != Vedtaksresultat.INGEN_TILBAKEBETALING,
            tilbakekrevingId = tilbakekrevingId,
            beregningsresultat = resultat.tilFrontendDto().beregningsresultatsperioder,
            hjemlerForTilbakekreving = listOf(HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15) +
                ytelse.hjemlerForTilbakekreving() +
                foreldelsesteg.hjemlerForTilbakekreving() +
                vilkårsvurderingsteg.hjemlerForTilbakekreving(),
            beregnerSkatt = ytelse.beregnerSkatt,
        )
    }

    private fun SideeffektContext.logg(
        behandlingsloggstype: Behandlingsloggstype,
        vararg ekstraInfo: Pair<EkstraInfo, Any>,
        rolle: Rolle = this.behandler.rolle,
    ) {
        behandlingslogg.lagre(
            LoggInnslag.opprett(
                behandlingId = id,
                opprettetTid = klokke.nå(),
                behandlingsloggstype = behandlingsloggstype,
                rolle = rolle,
                behandlerIdent = behandler.ident,
                ekstraInfo = ekstraInfo.toMap(),
            ),
        )
    }

    internal fun nyForhåndsvarselTilFrontend(varselbrev: Varselbrev?): ForhaandsvarselResponseDto {
        return forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev)
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
            klokke: Klokke,
        ): Behandling {
            val opprettet = klokke.nå()
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
                foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry),
                faktasteg = Faktasteg.opprett(
                    eksternFagsakRevurdering = eksternFagsakRevurdering.entry,
                    kravgrunnlag = kravgrunnlag.entry,
                    brevHistorikk = brevHistorikk,
                ),
                vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry),
                foreslåVedtakSteg = ForeslåVedtakSteg.opprett(),
                fatteVedtakSteg = FatteVedtakSteg.opprett(),
                forhåndsvarsel = Forhåndsvarsel.opprett(),
                forrigeBehandlingsstatus = BehandlingsstatusModell.OPPRETTET,
            )
        }
    }
}
