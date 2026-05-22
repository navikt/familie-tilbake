package no.nav.tilbakekreving

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.path
import no.nav.tilbakekreving.Klokke
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
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.behandlingslogg.LoggInnslag
import no.nav.tilbakekreving.behandlingslogg.Rolle
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.bigquery.BigQueryService
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
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
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
    private val behovObservatør: BehovObservatør,
    private val endringObservatør: EndringObservatør,
    var bruker: Bruker? = null,
    internal var tilstand: Tilstand,
    val bigQueryService: BigQueryService,
    val features: FeatureToggles,
    val behandlingslogg: Behandlingslogg,
    val klokke: Klokke,
) : FrontendDto<FagsakDto>, BehandlingObservatør {
    internal fun byttTilstand(nyTilstand: Tilstand) {
        tilstand = nyTilstand
        oppdaterPåminnelsestidspunkt()
        tilstand.entering(this)
    }

    fun oppdaterPåminnelsestidspunkt() {
        nestePåminnelse = tilstand.tidTilPåminnelse?.let(klokke.nå()::plus)
    }

    fun håndter(opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse) {
        tilstand.håndter(this, opprettTilbakekrevingEvent)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.TILBAKEKREVING_OPPRETTET,
                rolle = Rolle.VEDTAKSLØSNING,
                behandler = Behandler.Vedtaksløsning,
                behandlingId = null,
            ),
        )
    }

    fun håndter(kravgrunnlag: KravgrunnlagHendelse) {
        tilstand.håndter(this, kravgrunnlag)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.KRAVGRUNNLAG_MOTTATT,
                rolle = Rolle.VEDTAKSLØSNING,
                behandler = Behandler.Vedtaksløsning,
                behandlingId = null,
            ),
        )
    }

    fun håndter(fagsysteminfo: FagsysteminfoHendelse) {
        tilstand.håndter(this, fagsysteminfo)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.FAGSYSTEMINFO_OPPDATERT,
                rolle = Rolle.VEDTAKSLØSNING,
                behandler = Behandler.Vedtaksløsning,
                behandlingId = null,
            ),
        )
    }

    fun håndter(brukerinfo: BrukerinfoHendelse) {
        tilstand.håndter(this@Tilbakekreving, brukerinfo)
        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            behandlingslogg.lagre(
                opprettLoggInnslag(
                    behandlingsloggstype = Behandlingsloggstype.BRUKERINFO_OPPDATERT,
                    rolle = Rolle.VEDTAKSLØSNING,
                    behandler = Behandler.Vedtaksløsning,
                    behandlingId = null,
                ),
            )
        }
    }

    fun håndter(hendelse: VarselbrevJournalføringHendelse) {
        tilstand.håndter(this, hendelse)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.VARSELBREV_JOURNALFØRT,
                rolle = Rolle.VEDTAKSLØSNING,
                behandler = Behandler.Vedtaksløsning,
                behandlingId = behandlingHistorikk.nåværende().entry.id,
                EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
                EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
            ),
        )
    }

    fun håndter(hendelse: VarselbrevDistribueringHendelse) {
        tilstand.håndter(this, hendelse)
        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            behandlingslogg.lagre(
                opprettLoggInnslag(
                    behandlingsloggstype = Behandlingsloggstype.FORHÅNDSVARSEL_SENDT,
                    rolle = Rolle.VEDTAKSLØSNING,
                    behandler = Behandler.Vedtaksløsning,
                    behandlingId = id,
                    EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
                    EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
                ),
            )
        }
    }

    fun håndter(iverksettelseHendelse: IverksettelseHendelse) {
        tilstand.håndter(this, iverksettelseHendelse)
    }

    fun håndter(hendelse: JournalføringHendelse) {
        tilstand.håndter(this, hendelse)
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.VEDTAKSBREV_JOURNALFØRT,
                rolle = Rolle.VEDTAKSLØSNING,
                behandler = Behandler.Vedtaksløsning,
                behandlingId = hendelse.behandlingId,
                EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
                EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
            ),
        )
    }

    fun håndter(hendelse: DistribusjonHendelse) {
        tilstand.håndter(this, hendelse)

        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            behandlingslogg.lagre(
                opprettLoggInnslag(
                    behandlingsloggstype = Behandlingsloggstype.VEDTAKSBREV_SENDT,
                    rolle = Rolle.VEDTAKSLØSNING,
                    behandler = Behandler.Vedtaksløsning,
                    behandlingId = id,
                    EkstraInfo.JOURNALPOST_ID to hendelse.journalpostId,
                    EkstraInfo.DOKUMENTINFO_ID to hendelse.dokumentInfoId,
                ),
            )
        }
    }

    fun håndter(påminnelse: Påminnelse) {
        tilstand.håndter(this, påminnelse)
        oppdaterPåminnelsestidspunkt()
    }

    internal fun hånterEndretKravgrunnlag(kravgrunnlagHendelse: KravgrunnlagHendelse) {
        behandlingHistorikk.nåværende().entry.utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            kravgrunnlagHistorikk.lagre(kravgrunnlagHendelse)
            fåttNyttKravgrunnlag(kravgrunnlagHistorikk.nåværende())
        }
    }

    fun oppdaterFagsysteminfo(fagsysteminfo: FagsysteminfoHendelse) {
        val eksternFagsak = eksternFagsak.lagre(fagsysteminfo)
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.oppdaterEksternFagsak(eksternFagsak, behandlingslogg)
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

    fun håndterNullstilling(behandlingId: UUID, behandler: Behandler) {
        tilstand.håndterNullstilling(hentBehandling(behandlingId), sporingsinformasjon(), behandlingslogg, behandler)
    }

    fun håndterTrekkTilbakeFraGodkjenning(behandlingId: UUID, behandler: Behandler) {
        tilstand.håndterTrekkTilbakeFraGodkjenning(hentBehandling(behandlingId), sporingsinformasjon(), behandlingslogg, behandler)
    }

    fun opprettBehandling(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        behandler: Behandler,
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
            ansvarligSaksbehandler = behandler,
            eksternFagsakRevurdering = eksternFagsakRevurdering,
            kravgrunnlag = kravgrunnlagHistorikk.nåværende(),
            brevHistorikk = brevHistorikk,
            klokke = klokke,
        )
        behandling.utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            behandlingHistorikk.lagre(behandling)
            behandlingslogg.lagre(
                opprettLoggInnslag(
                    behandlingsloggstype = Behandlingsloggstype.BEHANDLING_OPPRETTET,
                    rolle = Rolle.VEDTAKSLØSNING,
                    behandler = Behandler.Vedtaksløsning,
                    behandlingId = behandlingId,
                ),
            )
        }
    }

    fun opprettBruker(aktør: Aktør) {
        this.bruker = Bruker(
            aktør = aktør,
        )
    }

    fun opprettBehandlingUtenIntegrasjon() {
        val kravgrunnlag = kravgrunnlagHistorikk.nåværende().entry
        // Å bruke kravgrunnlagreferanse er nok ikke alltid riktig her, men de fleste fagsystem bruker behandlingsid som referanse i kravgrunnlaget.
        val eksternBehandling = eksternFagsak.lagreTomBehandling(kravgrunnlag.fagsystemVedtaksdato, kravgrunnlag.referanse)
        opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning, null)
        opprettBruker(kravgrunnlag.vedtakGjelder)
        byttTilstand(AvventerBrukerinfo)
    }

    fun trengerVarselbrev(behandlingId: UUID, varseltekstFraSaksbehandler: String) {
        val behandling = hentBehandling(behandlingId)
        val varselbrev = behandling.opprettVarselbrev(varseltekstFraSaksbehandler, features)
        brevHistorikk.lagre(varselbrev)
        behandling.lagreOpprinneligFrist(varselbrev.fristForUttalelse)
        byttTilstand(SendVarselbrev)
    }

    fun sendVarselbrev(behandlingId: UUID, varseltekstFraSaksbehandler: String) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            val varselbrev = opprettVarselbrev(varseltekstFraSaksbehandler, features)
            nullstillForhåndsvarselUnntakOgUttalelse()
            brevHistorikk.lagre(varselbrev)
            lagreOpprinneligFrist(varselbrev.fristForUttalelse)
            byttTilstand(SendVarselbrev)
        }
    }

    fun trengerVarselbrevJournalføring() {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.trengerVarselbrevJournalføring(
            behovObservatør = behovObservatør,
            eksternFagsak = eksternFagsak,
            brukerinfo = bruker!!.hentBrukerinfo(),
            varselbrev = brevHistorikk.sisteVarselbrev()!!,
            varselbrevInfo = hentVarselbrevInfo(behandling.id),
        )
    }

    fun trengerBrukerinfo() {
        bruker!!.trengerBrukerinfo(behovObservatør, eksternFagsak.ytelse)
    }

    fun trengerIverksettelse() {
        behandlingHistorikk.nåværende().entry.trengerIverksettelse(
            behovObservatør,
            ytelse = eksternFagsak.ytelse,
            aktør = requireNotNull(bruker) { "Aktør kreves for Iverksettelse." }.aktør,
        )
    }

    fun opprettVedtaksbrev() {
        brevHistorikk.lagre(Vedtaksbrev.opprett(klokke))
    }

    fun trengerVedtaksbrevJournalføring() {
        behandlingHistorikk.nåværende().entry.trengerVedtaksbrevJournalføring(
            behovObservatør = behovObservatør,
            brevId = brevHistorikk.nåværende().entry.id,
            ytelse = eksternFagsak.ytelse,
            bruker = requireNotNull(bruker) { "Bruker kreves for journalføring av vedtaksbrev." },
            fagsakId = eksternFagsak.eksternId,
            tilbakekrevingId = id,
        )
    }

    fun trengerVarselbrevDistribusjon() {
        val behandling = behandlingHistorikk.nåværende().entry

        behandling.trengerVarselbrevDistribusjon(
            behovObservatør,
            journalpostId = brevHistorikk.sisteVarselbrev()!!.journalpostId!!,
            ytelse = eksternFagsak.ytelse,
            brevId = brevHistorikk.sisteVarselbrev()!!.id,
            fagsakId = eksternFagsak.eksternId,
            dokumentInfoId = brevHistorikk.sisteVarselbrev()!!.dokumentInfoId!!,
        )
    }

    fun trengerVedtaksbrevDistribusjon() {
        behandlingHistorikk.nåværende().entry.trengerVedtaksbrevDistribusjon(
            behovObservatør,
            journalpostId = brevHistorikk.nåværende().entry.journalpostId!!,
            brevId = brevHistorikk.nåværende().entry.id,
            fagsystem = eksternFagsak.ytelse.tilFagsystemDTO(),
            fagsakId = eksternFagsak.eksternId,
            dokumentInfoId = brevHistorikk.nåværende().entry.dokumentInfoId!!,
        )
    }

    fun trengerFagsysteminfo() {
        val kravgrunnlag = kravgrunnlagHistorikk.nåværende().entry
        eksternFagsak.trengerFagsysteminfo(
            eksternBehandlingId = kravgrunnlag.referanse,
            vedtakGjelderId = kravgrunnlag.vedtakGjelder.ident,
        )
    }

    fun sendVedtakIverksatt() {
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

    override fun tilFrontendDto(): FagsakDto {
        val eksternFagsakDto = eksternFagsak.tilFrontendDto()
        return FagsakDto(
            eksternFagsakId = eksternFagsakDto.eksternId,
            ytelsestype = eksternFagsakDto.ytelsestype,
            fagsystem = eksternFagsakDto.fagsystem,
            språkkode = bruker?.språkkode ?: Språkkode.NB,
            bruker = bruker.tilNullableFrontendDto(),
            behandlinger = behandlingHistorikk.tilOppsummeringDto(tilstand),
        )
    }

    fun faktastegFrontendDto(behandlingId: UUID): FaktaFeilutbetalingDto = hentBehandling(behandlingId).faktastegFrontendDto(opprettelsesvalg, opprettet)

    fun håndter(
        behandlingId: UUID,
        beslutter: Behandler,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            tilstand.håndter(this@Tilbakekreving, this, beslutter, vurderinger)
        }
    }

    fun håndter(
        behandlingId: UUID,
        behandler: Behandler,
        vurdering: Faktasteg.Vurdering,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            håndter(behandler, vurdering, this@Tilbakekreving, behandlingslogg)
        }
    }

    fun vurderFakta(
        behandlingId: UUID,
        behandler: Behandler,
        oppdaget: OppdagetDto?,
        årsak: String?,
        perioder: List<OppdaterFaktaPeriodeDto>?,
    ) {
        behandlingHistorikk.finn(behandlingId, sporingsinformasjon()).entry.utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            håndter(behandler, oppdaget, årsak, perioder, behandlingslogg)
        }
    }

    fun håndter(
        behandlingId: UUID,
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            håndter(behandler, periode, vurdering, this@Tilbakekreving, behandlingslogg)
        }
    }

    fun håndter(
        behandlingId: UUID,
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            håndter(behandler, periode, vurdering, this@Tilbakekreving, behandlingslogg)
        }
    }

    fun håndterForeslåVedtak(
        behandlingId: UUID,
        behandler: Behandler,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            håndterForeslåVedtak(behandler, this@Tilbakekreving, behandlingslogg)
        }
    }

    fun påminnNåværendePeriode() {
        behandlingHistorikk.nåværende().entry.håndterPåminnelse(tilstand, this)
    }

    fun frontendDtoForBehandling(
        behandlingId: UUID,
        behandler: Behandler,
        kanBeslutte: Boolean,
    ) = behandlingHistorikk.finn(behandlingId, sporingsinformasjon(behandlingId)).entry.tilFrontendDto(tilstand, behandler, kanBeslutte)

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
            loggInnlagEntities = behandlingslogg.tilEntity(id),
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
            endringObservatør.behandlingsstatusOppdatert(
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
            endringObservatør.behandlingEndret(
                EndringObservatør.BehandlingEndret(
                    behandlingId = behandlingId,
                    vedtakGjelderId = bruker?.aktør?.ident ?: "Ukjent",
                    eksternFagsakId = eksternFagsak.eksternId,
                    ytelse = eksternFagsak.ytelse,
                    eksternBehandlingId = eksternFagsak.behandlinger.nåværende().entry.behandlingId(),
                    sakOpprettet = opprettet,
                    varselSendt = brevHistorikk.sisteVarselbrev()?.sendtTid,
                    venter = venter,
                    behandlingsstatus = behandlingsstatus.forenkletStatus(eksternFagsak.ytelse, features),
                    forrigeBehandlingsstatus = forrigeBehandlingsstatus?.forenkletStatus(eksternFagsak.ytelse, features),
                    totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
                    hentSaksbehandlingURL = ::hentTilbakekrevingUrl,
                    fullstendigPeriode = totalFeilutbetaltPeriode,
                ),
            )
        }
    }

    fun hentVarselbrevInfo(behandlingId: UUID): VarselbrevInfo {
        val behandling = hentBehandling(behandlingId)
        val varselbrev = behandling.opprettVarselbrev("", features)
        return VarselbrevInfo(
            brukerinfo = bruker!!.hentBrukerinfo(),
            forhåndsvarselinfo = behandling.hentForhåndsvarselinfo(),
            eksternFagsakId = eksternFagsak.eksternId,
            ytelseType = eksternFagsak.ytelse.tilYtelseDTO(),
            hjemlerForTilbakekreving = eksternFagsak.forhåndsvarselHjemlerForTilbakekreving(),
            varsletDato = varselbrev.sendtTid,
            opprinneligUttalelsesfrist = varselbrev.fristForUttalelse,
        )
    }

    fun hentForhåndsvarselFrontendDto(behandlingId: UUID): ForhåndsvarselDto {
        val behandling = hentBehandling(behandlingId)
        return ForhåndsvarselDto(
            varselbrevDto = brevHistorikk.sisteVarselbrev()?.tilFrontendDto(),
            brukeruttalelse = behandling.brukeruttaleserTilFrontendDto(),
            forhåndsvarselUnntak = behandling.forhåndsvarselUnntakTilFrontendDto(),
            utsettUttalelseFrist = behandling.utsettUttalelseFristTilFrontendDto(),
        )
    }

    fun nyHentForhåndsvarselFrontendDto(behandlingId: UUID): ForhaandsvarselResponseDto {
        val behandling = hentBehandling(behandlingId)
        val forhåndsvarselinfo = brevHistorikk.sisteVarselbrev()?.let {
            ForhaandsvarselInfoDto(
                tekstFraSaksbehandler = it.tekstFraSaksbehandler,
                varselbrevSendtTid = it.sendtTid,
            )
        }
        return behandling.nyForhåndsvarselTilFrontend(forhåndsvarselinfo)
    }

    fun tilFeilutbetalingFrontendDto(behandlingId: UUID): FaktaOmFeilutbetalingDto {
        return hentBehandling(behandlingId).nyFaktastegFrontendDto(
            varselbrev = brevHistorikk.sisteVarselbrev(),
        )
    }

    fun hentBehandlingslogg(): List<LogginnslagDto> {
        return behandlingslogg.tilFrontend()
    }

    fun lagreUttalelse(
        behandlingId: UUID,
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: UttalelseInfo?,
        kommentar: String?,
        behandler: Behandler,
    ) {
        hentBehandling(behandlingId).utførEndring(::tilstand, this, bigQueryService, eksternFagsak.ytelse, behandlingslogg) {
            lagreUttalelse(
                uttalelseVurdering = uttalelseVurdering,
                uttalelseInfo = uttalelseInfo,
                kommentar = kommentar,
                behandlingslogg = behandlingslogg,
                behandler = behandler,
            )
        }
    }

    fun lagreFristUtsettelse(
        behandlingId: UUID,
        nyFrist: LocalDate,
        begrunnelse: String,
        behandler: Behandler,
    ): UttalelsesfristDto {
        requireNotNull(brevHistorikk.sisteVarselbrev()) {
            "Kan ikke utsette frist når forhåndsvarsel ikke er sendt"
        }
        return hentBehandling(behandlingId).lagreFristUtsettelse(
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
            behandlingslogg = behandlingslogg,
            behandler = behandler,
        )
    }

    fun lagreForhåndsvarselUnntak(
        behandlingId: UUID,
        begrunnelseForUnntak: BegrunnelseForUnntak,
        beskrivelse: String,
        behandler: Behandler,
    ) {
        hentBehandling(behandlingId).lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
            behandler = behandler,
            behandlingslogg = behandlingslogg,
        )
    }

    fun loggAvsluttning() {
        behandlingslogg.lagre(
            opprettLoggInnslag(
                behandlingsloggstype = Behandlingsloggstype.BEHANDLING_AVSLUTTET,
                rolle = Rolle.VEDTAKSLØSNING,
                behandler = Behandler.Vedtaksløsning,
                behandlingId = behandlingHistorikk.nåværende().entry.id,
            ),
        )
    }

    fun opprettLoggInnslag(
        behandlingsloggstype: Behandlingsloggstype,
        rolle: Rolle,
        behandler: Behandler,
        behandlingId: UUID?,
        vararg ekstraInfo: Pair<EkstraInfo, Any>,
    ): LoggInnslag {
        return LoggInnslag.opprett(
            id = UUID.randomUUID(),
            behandlingId = behandlingId,
            behandlingsloggstype = behandlingsloggstype,
            opprettetTid = klokke.nå(),
            rolle = rolle,
            behandlerIdent = behandler.ident,
            ekstraInfo = mapOf(*ekstraInfo),
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
            behovObservatør: BehovObservatør,
            opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse,
            bigQueryService: BigQueryService,
            endringObservatør: EndringObservatør,
            features: FeatureToggles,
            klokke: Klokke = SystemKlokke,
        ): Tilbakekreving {
            val nå = klokke.nå()
            val tilbakekreving = Tilbakekreving(
                id = id,
                opprettet = nå,
                nestePåminnelse = nå.plus(Start.tidTilPåminnelse),
                opprettelsesvalg = opprettTilbakekrevingEvent.opprettelsesvalg,
                eksternFagsak = EksternFagsak(
                    id = UUID.randomUUID(),
                    eksternId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                    ytelse = opprettTilbakekrevingEvent.eksternFagsak.ytelse,
                    behovObservatør = behovObservatør,
                    behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
                ),
                behovObservatør = behovObservatør,
                behandlingHistorikk = BehandlingHistorikk(mutableListOf()),
                kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf()),
                brevHistorikk = BrevHistorikk(mutableListOf()),
                tilstand = Start,
                bigQueryService = bigQueryService,
                endringObservatør = endringObservatør,
                features = features,
                behandlingslogg = Behandlingslogg(mutableListOf()),
                klokke = klokke,
            )
            tilbakekreving.håndter(opprettTilbakekrevingEvent)
            return tilbakekreving
        }
    }
}
