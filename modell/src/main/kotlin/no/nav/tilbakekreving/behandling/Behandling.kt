package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.aktør.Bruker
import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.behandlingsstegstatus
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg.Companion.klarTilVisning
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.VarselbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.bigquery.BigQueryService
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
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdagetDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaPeriodeDto
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
    val foreldelsesteg: Foreldelsesteg,
    private val faktasteg: Faktasteg,
    private val vilkårsvurderingsteg: Vilkårsvurderingsteg,
    private val foreslåVedtakSteg: ForeslåVedtakSteg,
    private val fatteVedtakSteg: FatteVedtakSteg,
    private var påVent: PåVent?,
    private val forhåndsvarsel: Forhåndsvarsel,
) : Historikk.HistorikkInnslag<UUID> {
    internal fun nyFaktastegFrontendDto(varselbrev: Varselbrev?): FaktaOmFeilutbetalingDto = faktasteg.nyTilFrontendDto(kravgrunnlag.entry, eksternFagsakRevurdering.entry, varselbrev)

    private fun bigqueryData(tilstand: Tilstand, ytelse: String): BigQueryBehandlingDataDto {
        return BigQueryBehandlingDataDto(
            behandlingId = id.toString(),
            opprettetDato = opprettet,
            periode = fullstendigPeriode(),
            behandlingstype = type.name,
            ytelse = ytelse,
            beløp = totaltFeilutbetaltBeløp().toLong(),
            enhetNavn = enhet?.navn,
            enhetKode = enhet?.kode,
            status = tilstand.behandlingsstatus(this).name,
            resultat = hentVedtaksresultat()?.name,
        )
    }

    fun nullstillForhåndsvarselUnntakOgUttalelse() = forhåndsvarsel.nullstillUnntakOgUttalelse()

    fun fåttNyttKravgrunnlag(oppdatertKravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>) {
        if (!faktasteg.erKlar()) {
            kravgrunnlag = oppdatertKravgrunnlag
        } else {
            throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttetEtterBehandlingenErPåbegynt, sporingsinformasjon())
        }
    }

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

    val foreldelsestegDto: FrontendDto<VurdertForeldelseDto>
        get() = FrontendDto {
            foreldelsesteg.tilFrontendDto(kravgrunnlag.entry)
        }
    val vilkårsvurderingsstegDto: FrontendDto<VurdertVilkårsvurderingDto>
        get() = FrontendDto {
            vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag.entry, eksternFagsakRevurdering.entry, foreldelsesteg)
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
            forhåndsvarselEntity = forhåndsvarsel.tilEntity(id),
        )
    }

    fun sporingsinformasjon(): Sporing {
        return Sporing(eksternFagsakRevurdering.entry.eksternId, id.toString())
    }

    internal fun steg(): List<Saksbehandlingsteg> = listOf(
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

    fun hentVedtaksresultatForFrontend(): FrontendBeregningsresultatDto {
        return lagBeregning().oppsummer().tilFrontendDto()
    }

    fun trengerVarselbrevJournalføring(
        behovObservatør: BehovObservatør,
        eksternFagsak: EksternFagsak,
        brukerinfo: Brukerinfo,
        varselbrev: Varselbrev,
        varselbrevInfo: VarselbrevInfo,
    ) {
        behovObservatør.håndter(
            VarselbrevJournalføringBehov(
                brevId = varselbrev.id,
                brukerinfo = brukerinfo,
                behandlingId = id,
                varselbrev = varselbrev,
                revurderingsvedtaksdato = varselbrevInfo.forhåndsvarselinfo.revurderingsvedtaksdato,
                varseltekstFraSaksbehandler = varselbrev.tekstFraSaksbehandler,
                eksternFagsakId = varselbrevInfo.eksternFagsakId,
                ytelse = eksternFagsak.ytelse,
                behandlendeEnhet = varselbrevInfo.forhåndsvarselinfo.behandlendeEnhet,
                feilutbetaltBeløp = varselbrevInfo.forhåndsvarselinfo.beløp,
                feilutbetaltePerioder = varselbrevInfo.forhåndsvarselinfo.feilutbetaltePerioder,
                gjelderDødsfall = brukerinfo.dødsdato != null,
                hjemlerForTilbakekreving = varselbrevInfo.hjemlerForTilbakekreving,
            ),
        )
    }

    fun trengerIverksettelse(
        behovObservatør: BehovObservatør,
        ytelse: Ytelse,
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
                ytelse = ytelse,
                aktør = aktør,
                behandlingstype = type,
            ),
        )
    }

    fun trengerVedtaksbrevJournalføring(
        behovObservatør: BehovObservatør,
        brevId: UUID,
        ytelse: Ytelse,
        bruker: Bruker,
        fagsakId: String,
        tilbakekrevingId: String,
    ) {
        behovObservatør.håndter(
            VedtaksbrevJournalføringBehov(
                brevId = brevId,
                behandlingId = id,
                ytelse = ytelse,
                bruker = bruker.hentBrukerinfo(),
                fagsakId = fagsakId,
                journalførendeEnhet = enhet!!.kode,
                vedtaksbrevInfo = hentVedtaksbrevInfo(bruker, ytelse, tilbakekrevingId),
                vedtaksresultat = hentVedtaksresultat()!!,
                beslutter = fatteVedtakSteg.ansvarligBeslutter!!,
            ),
        )
    }

    fun trengerVarselbrevDistribusjon(
        bebehovObservatør: BehovObservatør,
        journalpostId: String,
        ytelse: Ytelse,
        brevId: UUID,
        fagsakId: String,
        dokumentInfoId: String,
    ) {
        bebehovObservatør.håndter(
            VarselbrevDistribusjonBehov(
                behandlingId = id,
                journalpostId = journalpostId,
                fagsakId = fagsakId,
                ytelse = ytelse,
                brevId = brevId,
                behandlerIdent = ansvarligSaksbehandler.ident,
                dokumentInfoId = dokumentInfoId,
            ),
        )
    }

    fun trengerVedtaksbrevDistribusjon(
        bebehovObservatør: BehovObservatør,
        journalpostId: String,
        dokumentInfoId: String,
        brevId: UUID,
        fagsystem: FagsystemDTO,
        fagsakId: String,
    ) {
        bebehovObservatør.håndter(
            VedtaksbrevDistribusjonBehov(
                behandlingId = id,
                brevId = brevId,
                journalpostId = journalpostId,
                fagsystem = fagsystem,
                fagsakId = fagsakId,
                dokumentInfoId = dokumentInfoId,
            ),
        )
    }

    private fun skalBesluttes(): Boolean {
        return steg()
            .takeWhile { it.type != Behandlingssteg.FATTE_VEDTAK }
            .all { it.erFullstendig() }
    }

    private fun kanEndres(behandler: Behandler, saksbehandlerKanBeslutte: Boolean): Boolean {
        return when {
            skalBesluttes() -> behandler != ansvarligSaksbehandler && saksbehandlerKanBeslutte
            else -> true
        }
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
            kanEndres = tilstand.kanEndresAvSaksbehandler && kanEndres(behandler, kanBeslutte),
            kanSetteTilbakeTilFakta = true,
            varselSendt = false,
            behandlingsstegsinfo = listOf(
                listOf(
                    BehandlingsstegsinfoDto(
                        Behandlingssteg.GRUNNLAG,
                        Behandlingsstegstatus.AUTOUTFØRT,
                    ),
                ),
                steg().klarTilVisning().map {
                    BehandlingsstegsinfoDto(
                        it.type,
                        it.behandlingsstegstatus(steg().klarTilVisning()),
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
        behandlingslogg: Behandlingslogg,
    ) {
        validerBehandlingstatus(håndtertSteg = "fakta", faktasteg)
        faktasteg.vurder(vurdering)
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.FAKTA_VURDERT,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    internal fun håndter(
        behandler: Behandler,
        oppdaget: OppdagetDto?,
        årsak: String?,
        perioder: List<OppdaterFaktaPeriodeDto>?,
        behandlingslogg: Behandlingslogg,
    ) {
        validerBehandlingstatus(håndtertSteg = "fakta", faktasteg)
        if (oppdaget != null) {
            faktasteg.vurder(oppdaget)
        }
        if (årsak != null) {
            faktasteg.vurder(årsak)
        }
        if (perioder != null) {
            faktasteg.vurder(perioder)
        }
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.FAKTA_VURDERT,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
        observatør: BehandlingObservatør,
        behandlingslogg: Behandlingslogg,
    ) {
        validerBehandlingstatus("vilkårsvurdering", vilkårsvurderingsteg)
        vilkårsvurderingsteg.vurder(periode, vurdering)
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.VILKÅRSVURDERING_VURDERT,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    internal fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
        observatør: BehandlingObservatør,
        behandlingslogg: Behandlingslogg,
    ) {
        validerBehandlingstatus("foreldelse", foreldelsesteg)
        foreldelsesteg.vurderForeldelse(periode, vurdering)
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.FORELDELSE_VURDERT,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    internal fun håndterForeslåVedtak(
        behandler: Behandler,
        observatør: BehandlingObservatør,
        behandlingslogg: Behandlingslogg,
    ) {
        val tidligereManglendeSteg = steg()
            .takeWhile { it.type != Behandlingssteg.FORESLÅ_VEDTAK }
            .filter { !it.erKlar() }
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
        validerBehandlingstatus("vedtaksforslag", foreslåVedtakSteg)
        foreslåVedtakSteg.håndter()
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.FORESLÅ_VEDTAK_VURDERT,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    internal fun håndter(
        beslutter: Behandler,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
        observatør: BehandlingObservatør,
        behandlingslogg: Behandlingslogg,
    ) {
        validerBehandlingstatus("behandlingsutfall", fatteVedtakSteg)
        for ((behandlingssteg, vurdering) in vurderinger) {
            fatteVedtakSteg.håndter(beslutter, ansvarligSaksbehandler, behandlingssteg, vurdering, sporingsinformasjon())
            if (vurdering is FatteVedtakSteg.Vurdering.Underkjent) {
                steg()
                    .filter { it.type == behandlingssteg }
                    .forEach { it.underkjennSteget() }
            }
        }
        if (kanUtbetales()) {
            behandlingslogg.lagre(
                opprettLoggInnslag(
                    behandlingsloggstype = Behandlingsloggstype.VEDTAK_FATTET,
                    rolle = Rolle.BESLUTTER,
                    behandler = beslutter,
                ),
            )
        }
        if (underkjentVedtak()) {
            foreslåVedtakSteg.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)
            behandlingslogg.lagre(
                opprettLoggInnslag(
                    behandlingsloggstype = Behandlingsloggstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER,
                    rolle = Rolle.BESLUTTER,
                    behandler = beslutter,
                ),
            )
        }
    }

    internal fun oppdaterEksternFagsak(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        behandlingslogg: Behandlingslogg,
    ) {
        if (sistEndret == opprettet) {
            this.eksternFagsakRevurdering = eksternFagsakRevurdering
            flyttTilbakeTilFakta(behandlingslogg, Behandler.Vedtaksløsning)
        }
    }

    internal fun oppdaterBehandlendeEnhet(enhetKode: String) {
        enhet = Enhet.forKode(enhetKode)
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

    fun kanUtbetales(): Boolean = fatteVedtakSteg.erFullstendig() && !fatteVedtakSteg.erVedtakUnderkjent()

    fun underkjentVedtak(): Boolean {
        return fatteVedtakSteg.erVedtakUnderkjent()
    }

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
        behandlendeEnhet = enhet,
        ansvarligSaksbehandler = ansvarligSaksbehandler,
        beløp = totaltFeilutbetaltBeløp().toLong(),
        feilutbetaltePerioder = listOf(fullstendigPeriode()),
        revurderingsvedtaksdato = eksternFagsakRevurdering.entry.vedtaksdato,
    )

    fun lagreUttalelse(
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: UttalelseInfo?,
        kommentar: String?,
        behandlingslogg: Behandlingslogg,
        behandler: Behandler,
    ) {
        forhåndsvarsel.lagreUttalelse(
            uttalelseVurdering = uttalelseVurdering,
            uttalelseInfo = uttalelseInfo,
            kommentar = kommentar,
        )

        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.BRUKER_UTTALELSE,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String, behandlingslogg: Behandlingslogg, behandler: Behandler): UttalelsesfristDto {
        val uttalelsesfrist = forhåndsvarsel.lagreFristUtsettelse(
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.UTSETT_UTTALELSESFRIST,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
                EkstraInfo.NY_FRIST_FOR_UTTALELSE to nyFrist,
                EkstraInfo.BEGRUNNELSE_FOR_UTSATT_FRIST to begrunnelse,
            ),
        )
        return uttalelsesfrist
    }

    fun brukeruttaleserTilFrontendDto(): BrukeruttalelseDto? {
        return forhåndsvarsel.brukeruttaleserTilFrontendDto()
    }

    fun utsettUttalelseFristTilFrontendDto(): FristUtsettelseDto? {
        return forhåndsvarsel.utsettUttalelseFristTilFrontendDto()
    }

    internal fun vurdertePerioderForBrev(): List<BegrunnetPeriode> {
        return vilkårsvurderingsteg.vurdertePerioderForBrev(steg().flatMap { it.meldingerTilSaksbehandler() }.toSet())
    }

    internal fun brevSignatur(): Signatur = Signatur(
        ansvarligSaksbehandlerIdent = ansvarligSaksbehandler.ident,
        ansvarligBeslutterIdent = fatteVedtakSteg.ansvarligBeslutter?.ident,
        ansvarligEnhet = enhet!!.navn,
    )

    fun oppdaterBehandler(ansvarligSaksbehandler: Behandler) {
        this.sistEndret = LocalDateTime.now()
        this.ansvarligSaksbehandler = ansvarligSaksbehandler
    }

    fun lagreForhåndsvarselUnntak(
        begrunnelseForUnntak: BegrunnelseForUnntak,
        beskrivelse: String,
        behandler: Behandler,
        behandlingslogg: Behandlingslogg,
    ) {
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
        )

        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.UNNTAK_FOR_UTTALELSE,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    fun forhåndsvarselUnntakTilFrontendDto(): ForhåndsvarselUnntakDto? {
        return forhåndsvarsel.forhåndsvarselUnntakTilFrontendDto()
    }

    internal fun utførSideeffekt(tilstand: Tilstand, observatør: BehandlingObservatør, bigQueryService: BigQueryService, ytelsesNavn: String) {
        observatør.behandlingOppdatert(
            behandlingId = id,
            eksternBehandlingId = eksternFagsakRevurdering.entry.eksternId,
            vedtaksresultat = hentVedtaksresultat(),
            behandlingstatus = tilstand.behandlingsstatus(this),
            venterPåBruker = påVent?.avventerBruker() ?: false,
            ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
            ansvarligBeslutter = fatteVedtakSteg.ansvarligBeslutter?.ident,
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.feilutbetaltBeløpForAllePerioder(),
            totalFeilutbetaltPeriode = fullstendigPeriode(),
            ansvarligEnhet = enhet?.kode,
        )
        bigQueryService.oppdaterBehandling(
            bigqueryData(tilstand, ytelsesNavn),
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

    fun fullstendigPeriode(): Datoperiode {
        val kravgrunnlagPerioder = kravgrunnlag.entry.datoperioder(eksternFagsakRevurdering.entry)
        return kravgrunnlagPerioder.minOf { it.fom } til kravgrunnlagPerioder.maxOf { it.tom }
    }

    fun flyttTilbakeTilFakta(behandlingslogg: Behandlingslogg, behandler: Behandler) {
        steg().forEach {
            it.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)
        }
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.BEHANDLING_NULLSTILLT,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    fun trekkTilbakeFraGodkjenning(behandlingslogg: Behandlingslogg, behandler: Behandler) {
        foreslåVedtakSteg.nullstill(kravgrunnlag.entry, eksternFagsakRevurdering.entry)
        oppdaterBehandler(behandler)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.TREKK_TILBAKE_FRA_GODKJENNING,
                rolle = Rolle.SAKSBEHANDLER,
                behandler = behandler,
            ),
        )
    }

    fun sendVedtakIverksatt(
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

    fun opprettVarselbrev(
        varseltekstFraSaksbehandler: String,
        features: FeatureToggles,
    ): Varselbrev {
        val varselbrev = Varselbrev.opprett(
            ansvarligSaksbehandlerIdent = ansvarligSaksbehandler.ident,
            kravgrunnlag = kravgrunnlag,
            varseltekstFraSaksbehandler = varseltekstFraSaksbehandler,
            features = features,
        )
        forhåndsvarsel.lagreOpprinneligFrist(varselbrev.fristForUttalelse)
        return varselbrev
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

    fun opprettLoggInnslag(
        behandlingsloggstype: Behandlingsloggstype,
        rolle: Rolle,
        behandler: Behandler,
        vararg ekstraInfo: Pair<EkstraInfo, Any>,
    ): LoggInnslag {
        return LoggInnslag(
            id = UUID.randomUUID(),
            opprettetTid = LocalDateTime.now(),
            behandlingsloggstype = behandlingsloggstype,
            rolle = rolle,
            behandlerIdent = behandler.ident,
            behandlingId = id,
            ekstraInfo = mapOf(*ekstraInfo),
        )
    }

    fun nyForhåndsvarselTilFrontend(forhåndsvarselinfo: ForhaandsvarselInfoDto?): ForhaandsvarselResponseDto {
        return forhåndsvarsel.nyForhåndsvarselTilFrontend(forhåndsvarselinfo)
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
            bigQueryService: BigQueryService,
            ytelsesNavn: String,
        ): Behandling {
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
                foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry),
                faktasteg = Faktasteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry, brevHistorikk),
                vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(eksternFagsakRevurdering.entry, kravgrunnlag.entry),
                foreslåVedtakSteg = ForeslåVedtakSteg.opprett(),
                fatteVedtakSteg = FatteVedtakSteg.opprett(),
                påVent = null,
                forhåndsvarsel = Forhåndsvarsel.opprett(),
            ).also {
                it.utførSideeffekt(tilstand, behandlingObservatør, bigQueryService, ytelsesNavn)
            }
        }
    }
}
