package no.nav.tilbakekreving

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.path
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.aktør.Bruker
import no.nav.tilbakekreving.aktør.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behandling.BehandlingObservatør
import no.nav.tilbakekreving.behandling.Behandlingsinformasjon
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentTypeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdagetDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.SendVarselbrev
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.TilBehandling
import no.nav.tilbakekreving.tilstand.Tilstand
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Tilbakekreving internal constructor(
    val id: String,
    val eksternFagsak: EksternFagsak,
    private val behandlingHistorikk: BehandlingHistorikk,
    val kravgrunnlagHistorikk: KravgrunnlagHistorikk,
    val brevHistorikk: BrevHistorikk,
    val opprettet: LocalDateTime,
    private var nestePåminnelse: LocalDateTime?,
    val opprettelsesvalg: Opprettelsesvalg,
    var bruker: Bruker? = null,
    internal var tilstand: Tilstand,
) : BehandlingObservatør {
    internal fun byttTilstand(nyTilstand: Tilstand, sideeffektContext: SideeffektContext) {
        tilstand = nyTilstand
        oppdaterPåminnelsestidspunkt(sideeffektContext.klokke)
        tilstand.entering(this, sideeffektContext)
    }

    fun oppdaterPåminnelsestidspunkt(klokke: Klokke) {
        nestePåminnelse = tilstand.tidTilPåminnelse?.let(klokke.nå()::plus)
    }

    fun håndter(opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, opprettTilbakekrevingEvent, sideeffektContext)
        sideeffektContext.logg(
            behandlingsloggstype = Behandlingsloggstype.TILBAKEKREVING_OPPRETTET,
            behandlingId = null,
        )
    }

    fun håndter(kravgrunnlag: KravgrunnlagHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, kravgrunnlag, sideeffektContext)
        sideeffektContext.logg(
            behandlingsloggstype = Behandlingsloggstype.KRAVGRUNNLAG_MOTTATT,
            behandlingId = null,
        )
    }

    fun håndter(fagsysteminfo: FagsysteminfoHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, fagsysteminfo, sideeffektContext)
        sideeffektContext.logg(
            behandlingsloggstype = Behandlingsloggstype.FAGSYSTEMINFO_OPPDATERT,
            behandlingId = null,
        )
    }

    fun håndter(brukerinfo: BrukerinfoHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this@Tilbakekreving, brukerinfo, sideeffektContext)
        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            sideeffektContext.logg(
                behandlingsloggstype = Behandlingsloggstype.BRUKERINFO_OPPDATERT,
                behandlingId = null,
            )
        }
    }

    fun håndter(hendelse: VarselbrevJournalføringHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, hendelse, sideeffektContext)
        sideeffektContext.logg(
            behandlingsloggstype = Behandlingsloggstype.VARSELBREV_JOURNALFØRT,
            behandlingId = behandlingHistorikk.nåværende().entry.id,
            EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
            EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
        )
    }

    fun håndter(hendelse: VarselbrevDistribueringHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, hendelse, sideeffektContext)
        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            sideeffektContext.logg(
                behandlingsloggstype = Behandlingsloggstype.FORHÅNDSVARSEL_SENDT,
                behandlingId = id,
                EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
                EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
            )
        }
    }

    fun håndter(iverksettelseHendelse: IverksettelseHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, iverksettelseHendelse, sideeffektContext)
    }

    fun håndter(hendelse: JournalføringHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, hendelse, sideeffektContext)
        sideeffektContext.logg(
            behandlingsloggstype = Behandlingsloggstype.VEDTAKSBREV_JOURNALFØRT,
            behandlingId = hendelse.behandlingId,
            EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
            EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
        )
    }

    fun håndter(hendelse: DistribusjonHendelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, hendelse, sideeffektContext)

        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            sideeffektContext.logg(
                behandlingsloggstype = Behandlingsloggstype.VEDTAKSBREV_SENDT,
                behandlingId = id,
                EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
                EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
            )
        }
    }

    fun håndter(påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilstand.håndter(this, påminnelse, sideeffektContext)
        oppdaterPåminnelsestidspunkt(sideeffektContext.klokke)
    }

    internal fun hånterEndretKravgrunnlag(kravgrunnlagHendelse: KravgrunnlagHendelse, sideeffektContext: SideeffektContext) {
        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            kravgrunnlagHistorikk.lagre(kravgrunnlagHendelse)
            oppdaterKravgrunnlag(kravgrunnlagHistorikk.nåværende(), sideeffektContext.klokke)
        }
    }

    fun oppdaterFagsysteminfo(fagsysteminfo: FagsysteminfoHendelse, sideeffektContext: SideeffektContext) {
        val eksternFagsak = eksternFagsak.lagre(fagsysteminfo)
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.oppdaterEksternFagsak(eksternFagsak, sideeffektContext)
        if (fagsysteminfo.behandlendeEnhet != null) {
            behandling.oppdaterBehandlendeEnhet(fagsysteminfo.behandlendeEnhet)
        }
    }

    fun sporingsinformasjon(behandlingId: UUID? = null): Sporing {
        return Sporing(
            eksternFagsak.eksternId,
            behandlingId?.toString() ?: if (behandlingHistorikk.harBehandling()) {
                behandlingHistorikk.nåværende().entry.id.toString()
            } else {
                null
            },
        )
    }

    fun hentBehandling(behandlingId: UUID): Behandling {
        return behandlingHistorikk.finn(behandlingId, Sporing(id, behandlingId.toString())).entry
    }

    fun håndterNullstilling(behandlingId: UUID, sideeffektContext: SideeffektContext) {
        tilstand.håndterNullstilling(hentBehandling(behandlingId), sporingsinformasjon(), sideeffektContext)
    }

    fun håndterTrekkTilbakeFraGodkjenning(behandlingId: UUID, sideeffektContext: SideeffektContext) {
        tilstand.håndterTrekkTilbakeFraGodkjenning(hentBehandling(behandlingId), sporingsinformasjon(), sideeffektContext)
    }

    fun opprettBehandling(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        sideeffektContext: SideeffektContext,
        behandlendeEnhet: String?,
    ) {
        if (bruker == null) {
            opprettBruker(kravgrunnlagHistorikk.nåværende().entry.vedtakGjelder)
        }
        val behandlingId = UUID.randomUUID()
        val behandling = Behandling.nyBehandling(
            id = behandlingId,
            type = Behandlingstype.TILBAKEKREVING,
            enhet = behandlendeEnhet?.let(Enhet::forKode),
            ansvarligSaksbehandler = sideeffektContext.behandler,
            eksternFagsakRevurdering = eksternFagsakRevurdering,
            kravgrunnlag = kravgrunnlagHistorikk.nåværende(),
            brevHistorikk = brevHistorikk,
            klokke = sideeffektContext.klokke,
        )
        behandling.utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            behandlingHistorikk.lagre(behandling)
            sideeffektContext.logg(
                behandlingsloggstype = Behandlingsloggstype.BEHANDLING_OPPRETTET,
                behandlingId = behandlingId,
            )
        }
    }

    fun opprettBruker(aktør: Aktør) {
        this.bruker = Bruker(
            aktør = aktør,
        )
    }

    fun opprettBehandlingUtenIntegrasjon(sideeffektContext: SideeffektContext) {
        val kravgrunnlag = kravgrunnlagHistorikk.nåværende().entry
        // Å bruke kravgrunnlagreferanse er nok ikke alltid riktig her, men de fleste fagsystem bruker behandlingsid som referanse i kravgrunnlaget.
        val eksternBehandling = eksternFagsak.lagreTomBehandling(kravgrunnlag.fagsystemVedtaksdato, kravgrunnlag.referanse)
        opprettBehandling(eksternBehandling, sideeffektContext, null)
        opprettBruker(kravgrunnlag.vedtakGjelder)
        byttTilstand(AvventerBrukerinfo, sideeffektContext)
    }

    fun sendVarselbrev(behandlingId: UUID, varseltekstFraSaksbehandler: String, sideeffektContext: SideeffektContext) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            val varselbrev = opprettVarselbrev(varseltekstFraSaksbehandler, sideeffektContext)
            nullstillForhåndsvarselUnntakOgUttalelse()
            brevHistorikk.lagre(varselbrev)
            lagreOpprinneligFrist(varselbrev.fristForUttalelse)
            byttTilstand(SendVarselbrev, sideeffektContext)
        }
    }

    fun trengerVarselbrevJournalføring(sideeffektContext: SideeffektContext) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.trengerVarselbrevJournalføring(
            sideeffektContext = sideeffektContext,
            eksternFagsak = eksternFagsak,
            brukerinfo = bruker!!.hentBrukerinfo(),
            varselbrevInfo = brevHistorikk.sisteVarselbrev()!!.tilVarselbrevInfo(bruker!!, behandling.hentForhåndsvarselinfo(), eksternFagsak),
        )
    }

    fun trengerBrukerinfo(sideeffektContext: SideeffektContext) {
        sideeffektContext.behovObservatør.håndter(bruker!!.brukerinfoBehov(eksternFagsak.ytelse))
    }

    fun trengerIverksettelse(sideeffektContext: SideeffektContext) {
        behandlingHistorikk.nåværende().entry.trengerIverksettelse(
            sideeffektContext = sideeffektContext,
            ytelse = eksternFagsak.ytelse,
            aktør = requireNotNull(bruker) { "Aktør kreves for Iverksettelse." }.aktør,
        )
    }

    fun opprettVedtaksbrev(klokke: Klokke) {
        brevHistorikk.lagre(Vedtaksbrev.opprett(klokke))
    }

    fun trengerVedtaksbrevJournalføring(sideeffektContext: SideeffektContext) {
        behandlingHistorikk.nåværende().entry.trengerVedtaksbrevJournalføring(
            sideeffektContext = sideeffektContext,
            brevId = brevHistorikk.nåværende().entry.id,
            ytelse = eksternFagsak.ytelse,
            bruker = requireNotNull(bruker) { "Bruker kreves for journalføring av vedtaksbrev." },
            fagsakId = eksternFagsak.eksternId,
            tilbakekrevingId = id,
        )
    }

    fun trengerVarselbrevDistribusjon(sideeffektContext: SideeffektContext) {
        sideeffektContext.behovObservatør.håndter(
            behandlingHistorikk.nåværende().entry.lagVarselbrevDistribusjonBehov(
                journalpostId = brevHistorikk.sisteVarselbrev()!!.journalpostId!!,
                ytelse = eksternFagsak.ytelse,
                brevId = brevHistorikk.sisteVarselbrev()!!.id,
                fagsakId = eksternFagsak.eksternId,
                dokumentInfoId = brevHistorikk.sisteVarselbrev()!!.dokumentInfoId!!,
            ),
        )
    }

    fun trengerVedtaksbrevDistribusjon(sideeffektContext: SideeffektContext) {
        sideeffektContext.behovObservatør.håndter(
            behandlingHistorikk.nåværende().entry.lagVedtaksbrevDistribusjonBehov(
                journalpostId = brevHistorikk.nåværende().entry.journalpostId!!,
                brevId = brevHistorikk.nåværende().entry.id,
                fagsystem = eksternFagsak.ytelse.tilFagsystemDTO(),
                fagsakId = eksternFagsak.eksternId,
                dokumentInfoId = brevHistorikk.nåværende().entry.dokumentInfoId!!,
            ),
        )
    }

    fun trengerFagsysteminfo(sideeffektContext: SideeffektContext) {
        val kravgrunnlag = kravgrunnlagHistorikk.nåværende().entry
        sideeffektContext.behovObservatør.håndter(
            eksternFagsak.fagsysteminfoBehov(
                eksternBehandlingId = kravgrunnlag.referanse,
                vedtakGjelderId = kravgrunnlag.vedtakGjelder.ident,
            ),
        )
    }

    fun sendVedtakIverksatt(endringObservatør: EndringObservatør) {
        val nåværendeBehandling = behandlingHistorikk.nåværende().entry
        nåværendeBehandling.sendVedtakIverksatt(
            forrigeBehandlingId = behandlingHistorikk.forrige()?.entry?.id,
            eksternFagsystemId = eksternFagsak.eksternId,
            ytelse = eksternFagsak.ytelse,
            endringObservatør = endringObservatør,
            ansvarligEnhet = nåværendeBehandling.hentBehandlingsinformasjon().enhet?.kode,
        )
    }

    fun hentBehandlingsinformasjon(): Behandlingsinformasjon = behandlingHistorikk.nåværende().entry.hentBehandlingsinformasjon()

    fun hentVedtaksbrevInfo(behandlingId: UUID): VedtaksbrevInfo {
        return behandlingHistorikk.entry(behandlingId).hentVedtaksbrevInfo(
            bruker = requireNotNull(bruker) { { "Bruker kreves for å hente vedtaksbrev informajson." } },
            ytelse = eksternFagsak.ytelse,
            tilbakekrevingId = id,
        )
    }

    fun tilFrontendDto(klokke: Klokke): FagsakDto {
        val eksternFagsakDto = eksternFagsak.tilFrontendDto()
        return FagsakDto(
            eksternFagsakId = eksternFagsakDto.eksternId,
            ytelsestype = eksternFagsakDto.ytelsestype,
            fagsystem = eksternFagsakDto.fagsystem,
            språkkode = bruker?.språkkode ?: Språkkode.NB,
            bruker = bruker.tilNullableFrontendDto(),
            behandlinger = behandlingHistorikk.tilOppsummeringDto(tilstand, klokke),
        )
    }

    fun faktastegFrontendDto(behandlingId: UUID): FaktaFeilutbetalingDto = hentBehandling(behandlingId).faktastegFrontendDto(opprettelsesvalg, opprettet)

    fun håndter(
        behandlingId: UUID,
        sideeffektContext: SideeffektContext,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            tilstand.håndter(this@Tilbakekreving, this, vurderinger, sideeffektContext)
        }
    }

    fun håndter(
        behandlingId: UUID,
        sideeffektContext: SideeffektContext,
        vurdering: Faktasteg.Vurdering,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            håndter(sideeffektContext, vurdering)
        }
    }

    fun vurderFakta(
        behandlingId: UUID,
        sideeffektContext: SideeffektContext,
        oppdaget: OppdagetDto?,
        årsak: String?,
        perioder: List<OppdaterFaktaPeriodeDto>?,
    ) {
        behandlingHistorikk.finn(behandlingId, sporingsinformasjon()).entry.utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            håndter(sideeffektContext, oppdaget, årsak, perioder)
        }
    }

    fun håndter(
        behandlingId: UUID,
        sideeffektContext: SideeffektContext,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            håndter(sideeffektContext, periode, vurdering)
        }
    }

    fun håndter(
        behandlingId: UUID,
        sideeffektContext: SideeffektContext,
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            håndter(sideeffektContext, periode, vurdering)
        }
    }

    fun håndterForeslåVedtak(
        behandlingId: UUID,
        sideeffektContext: SideeffektContext,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            håndterForeslåVedtak(sideeffektContext)
        }
    }

    fun påminnNåværendePeriode(sideeffektContext: SideeffektContext) {
        behandlingHistorikk.nåværende().entry.håndterPåminnelse(tilstand, sideeffektContext, this)
    }

    fun frontendDtoForBehandling(
        behandlingId: UUID,
        sideeffektContext: LesContext,
        kanBeslutte: Boolean,
    ) = behandlingHistorikk.finn(behandlingId, sporingsinformasjon(behandlingId)).entry.tilFrontendDto(tilstand, sideeffektContext, kanBeslutte)

    fun tilEntity(): TilbakekrevingEntity {
        return TilbakekrevingEntity(
            nåværendeTilstand = tilstand.tilbakekrevingTilstand,
            id = this.id,
            eksternFagsak = this.eksternFagsak.tilEntity(id),
            behandlingHistorikkEntities = this.behandlingHistorikk.tilEntity(id),
            kravgrunnlagHistorikkEntities = this.kravgrunnlagHistorikk.tilEntity(id),
            brevHistorikkEntities = this.brevHistorikk.tilEntity(id),
            opprettet = this.opprettet,
            opprettelsesvalg = this.opprettelsesvalg,
            nestePåminnelse = nestePåminnelse,
            bruker = this.bruker?.tilEntity(id),
        )
    }

    fun hentTilbakekrevingUrl(baseUrl: String): String {
        return URLBuilder(baseUrl).apply {
            path("fagsystem", eksternFagsak.ytelse.tilFagsystemDTO().toString(), "fagsak", eksternFagsak.eksternId)
            if (behandlingHistorikk.harBehandling()) {
                appendPathSegments("behandling", behandlingHistorikk.nåværende().entry.id.toString())
            }
        }.buildString()
    }

    override fun behandlingOppdatert(
        sideeffektContext: SideeffektContext,
        behandlingId: UUID,
        eksternBehandlingId: String,
        vedtaksresultat: Vedtaksresultat?,
        behandlingsstatus: BehandlingsstatusModell,
        forrigeBehandlingsstatus: BehandlingsstatusModell?,
        venter: Venter?,
        ansvarligSaksbehandler: Behandler,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal,
        totalFeilutbetaltPeriode: Datoperiode,
        ansvarligEnhet: String?,
    ) {
        if (tilstand != TilBehandling || ansvarligSaksbehandler != Behandler.Vedtaksløsning) {
            sideeffektContext.endringObservatør.behandlingsstatusOppdatert(
                behandlingId = behandlingId,
                forrigeBehandlingId = behandlingHistorikk.forrige()?.entry?.id,
                eksternFagsystemId = eksternFagsak.eksternId,
                eksternBehandlingId = eksternBehandlingId,
                ytelse = eksternFagsak.ytelse,
                tilstand = tilstand.tilbakekrevingTilstand,
                behandlingstatus = behandlingsstatus.gammelFrontendDTO,
                vedtaksresultat = vedtaksresultat,
                venterPåBruker = venter != null,
                ansvarligEnhet = ansvarligEnhet,
                ansvarligSaksbehandler = ansvarligSaksbehandler.ident,
                ansvarligBeslutter = ansvarligBeslutter,
                totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
                totalFeilutbetaltPeriode = totalFeilutbetaltPeriode,
            )
        }
        if (behandlingsstatus.relevantForFagsystem) {
            sideeffektContext.endringObservatør.behandlingEndret(
                EndringObservatør.BehandlingEndret(
                    behandlingId = behandlingId,
                    vedtakGjelderId = bruker?.aktør?.ident ?: "Ukjent",
                    eksternFagsakId = eksternFagsak.eksternId,
                    ytelse = eksternFagsak.ytelse,
                    eksternBehandlingId = eksternFagsak.behandlinger.nåværende().entry.behandlingId(),
                    sakOpprettet = opprettet,
                    varselSendt = brevHistorikk.sisteVarselbrev()?.sendtTid,
                    venter = venter,
                    behandlingsstatus = behandlingsstatus.forenkletStatus(eksternFagsak.ytelse, sideeffektContext.features),
                    forrigeBehandlingsstatus = forrigeBehandlingsstatus?.forenkletStatus(eksternFagsak.ytelse, sideeffektContext.features),
                    totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
                    hentSaksbehandlingURL = ::hentTilbakekrevingUrl,
                    fullstendigPeriode = totalFeilutbetaltPeriode,
                ),
            )
        }
    }

    fun hentVarselbrevInfo(behandlingId: UUID, lesContext: LesContext): VarselbrevInfo {
        val behandling = hentBehandling(behandlingId)
        val varselbrev = behandling.opprettVarselbrev("", lesContext)
        return varselbrev.tilVarselbrevInfo(bruker!!, behandling.hentForhåndsvarselinfo(), eksternFagsak)
    }

    fun hentForhåndsvarselFrontendDto(behandlingId: UUID): ForhåndsvarselDto {
        return hentBehandling(behandlingId).forhåndsvarselFrontendDto(brevHistorikk.sisteVarselbrev())
    }

    fun nyHentForhåndsvarselFrontendDto(behandlingId: UUID): ForhaandsvarselResponseDto {
        return hentBehandling(behandlingId).nyForhåndsvarselTilFrontend(brevHistorikk.sisteVarselbrev())
    }

    fun tilFeilutbetalingFrontendDto(behandlingId: UUID, klokke: Klokke): FaktaOmFeilutbetalingDto {
        return hentBehandling(behandlingId).nyFaktastegFrontendDto(
            varselbrev = brevHistorikk.sisteVarselbrev(),
            klokke = klokke,
        )
    }

    fun lagreUttalelse(
        behandlingId: UUID,
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: UttalelseInfo?,
        kommentar: String?,
        sideeffektContext: SideeffektContext,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, sideeffektContext, this, eksternFagsak.ytelse) {
            lagreUttalelse(
                uttalelseVurdering = uttalelseVurdering,
                uttalelseInfo = uttalelseInfo,
                kommentar = kommentar,
                sideeffektContext = sideeffektContext,
            )
        }
    }

    fun lagreFristUtsettelse(
        behandlingId: UUID,
        nyFrist: LocalDate,
        begrunnelse: String,
        sideeffektContext: SideeffektContext,
    ): UttalelsesfristDto {
        requireNotNull(brevHistorikk.sisteVarselbrev()) {
            "Kan ikke utsette frist når forhåndsvarsel ikke er sendt"
        }
        return hentBehandling(behandlingId).lagreFristUtsettelse(
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
            sideeffektContext = sideeffektContext,
        )
    }

    fun lagreForhåndsvarselUnntak(
        behandlingId: UUID,
        begrunnelseForUnntak: BegrunnelseForUnntak,
        beskrivelse: String,
        sideeffektContext: SideeffektContext,
    ) {
        hentBehandling(behandlingId).lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
            sideeffektContext = sideeffektContext,
        )
    }

    fun loggAvsluttning(sideeffektContext: SideeffektContext) {
        sideeffektContext.logg(
            behandlingsloggstype = Behandlingsloggstype.BEHANDLING_AVSLUTTET,
            behandlingId = behandlingHistorikk.nåværende().entry.id,
        )
    }

    fun SideeffektContext.logg(
        behandlingsloggstype: Behandlingsloggstype,
        behandlingId: UUID?,
        vararg ekstraInfo: Pair<EkstraInfo, Any>,
    ) {
        behandlingslogg.lagre(
            LoggInnslag.opprett(
                behandlingId = behandlingId,
                behandlingsloggstype = behandlingsloggstype,
                opprettetTid = klokke.nå(),
                rolle = behandler.rolle,
                behandlerIdent = behandler.ident,
                ekstraInfo = mapOf(*ekstraInfo),
            ),
        )
    }

    fun hentDokumentInfo(dokumentType: DokumentTypeDto): DokumentInfoDto {
        val dokument = when (dokumentType) {
            DokumentTypeDto.VEDTAKSBREV -> brevHistorikk.sisteVedtaksbrev()
            DokumentTypeDto.VARSELBREV -> brevHistorikk.sisteVarselbrev()
        }
        return DokumentInfoDto(
            journalpostId = dokument?.journalpostId,
            dokumentId = dokument?.dokumentInfoId,
        )
    }

    companion object {
        fun opprett(
            id: String,
            opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse,
            sideeffektContext: SideeffektContext,
        ): Tilbakekreving {
            val nå = sideeffektContext.klokke.nå()
            val tilbakekreving = Tilbakekreving(
                id = id,
                opprettet = nå,
                nestePåminnelse = nå.plus(Start.tidTilPåminnelse),
                opprettelsesvalg = opprettTilbakekrevingEvent.opprettelsesvalg,
                eksternFagsak = EksternFagsak(
                    id = UUID.randomUUID(),
                    eksternId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                    ytelse = opprettTilbakekrevingEvent.eksternFagsak.ytelse,
                    behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
                ),
                behandlingHistorikk = BehandlingHistorikk(mutableListOf()),
                kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf()),
                brevHistorikk = BrevHistorikk(mutableListOf()),
                tilstand = Start,
            )
            tilbakekreving.håndter(opprettTilbakekrevingEvent, sideeffektContext)
            return tilbakekreving
        }
    }
}
