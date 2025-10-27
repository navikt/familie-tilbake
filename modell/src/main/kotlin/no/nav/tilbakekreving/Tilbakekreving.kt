package no.nav.tilbakekreving

import io.ktor.http.URLBuilder
import io.ktor.http.path
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.aktør.Bruker
import no.nav.tilbakekreving.aktør.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behandling.BehandlingObservatør
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.endring.EndringObservatør
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.AvventerBrukerinfo
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.Tilstand
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class Tilbakekreving internal constructor(
    val id: String,
    val eksternFagsak: EksternFagsak,
    val behandlingHistorikk: BehandlingHistorikk,
    val kravgrunnlagHistorikk: KravgrunnlagHistorikk,
    val brevHistorikk: BrevHistorikk,
    val opprettet: LocalDateTime,
    val opprettelsesvalg: Opprettelsesvalg,
    private val behovObservatør: BehovObservatør,
    private val endringObservatør: EndringObservatør,
    var bruker: Bruker? = null,
    internal var tilstand: Tilstand,
    val bigQueryService: BigQueryService,
) : FrontendDto<FagsakDto>, BehandlingObservatør {
    internal fun byttTilstand(nyTilstand: Tilstand) {
        tilstand = nyTilstand
        tilstand.entering(this)
    }

    fun håndter(opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse) {
        tilstand.håndter(this, opprettTilbakekrevingEvent)
    }

    fun håndter(kravgrunnlag: KravgrunnlagHendelse) {
        tilstand.håndter(this, kravgrunnlag)
    }

    fun håndter(fagsysteminfo: FagsysteminfoHendelse) {
        tilstand.håndter(this, fagsysteminfo)
    }

    fun håndter(brukerinfo: BrukerinfoHendelse) {
        tilstand.håndter(this, brukerinfo)
    }

    fun håndter(varselbrevSendt: VarselbrevSendtHendelse) {
        tilstand.håndter(this, varselbrevSendt)
    }

    fun håndter(iverksettelseHendelse: IverksettelseHendelse) {
        tilstand.håndter(this, iverksettelseHendelse)
        behandlingHistorikk.nåværende().entry.utførSideeffekt(tilstand, this)
    }

    fun oppdaterFagsysteminfo(fagsysteminfo: FagsysteminfoHendelse) {
        val eksternFagsak = eksternFagsak.lagre(fagsysteminfo)
        behandlingHistorikk.nåværende().entry
            .oppdaterEksternFagsak(eksternFagsak)
    }

    fun sporingsinformasjon(): Sporing {
        return Sporing(eksternFagsak.eksternId, behandlingHistorikk.nåværende().entry.id.toString())
    }

    fun håndterNullstilling() {
        val nåværendeBehandling = behandlingHistorikk.nåværende().entry
        tilstand.håndterNullstilling(nåværendeBehandling, sporingsinformasjon())
    }

    fun opprettBrevmottakerSteg(
        navn: String,
        ident: String,
    ) {
        behandlingHistorikk.nåværende().entry.opprettBrevmottaker(navn, ident)
    }

    fun opprettBehandling(
        eksternFagsakRevurdering: HistorikkReferanse<UUID, EksternFagsakRevurdering>,
        behandler: Behandler,
    ) {
        val behandlingId = UUID.randomUUID()
        val behandling = Behandling.nyBehandling(
            id = behandlingId,
            type = Behandlingstype.TILBAKEKREVING,
            enhet = null,
            ansvarligSaksbehandler = behandler,
            eksternFagsakRevurdering = eksternFagsakRevurdering,
            kravgrunnlag = kravgrunnlagHistorikk.nåværende(),
            brevHistorikk = brevHistorikk,
            behandlingObservatør = this,
            tilstand = tilstand,
        )
        behandlingHistorikk.lagre(behandling)

        val behandlingInfo = behandling.hentBehandlingsinformasjon()
        bigQueryService.leggeTilBehanlingInfo(
            behandlingId = behandlingInfo.behandlingId.toString(),
            opprettetTid = opprettet,
            ytelsestypeKode = hentFagsysteminfo().tilYtelsestype().kode,
            behandlingstype = behandlingInfo.behandlingstype.name,
            behandlendeEnhet = behandlingInfo.enhet?.kode,
        )
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
        opprettBehandling(eksternBehandling, Behandler.Vedtaksløsning)
        opprettBruker(kravgrunnlag.vedtakGjelder)
        byttTilstand(AvventerBrukerinfo)
    }

    fun trengerVarselbrev(varselbrevBehov: VarselbrevBehov) {
        behovObservatør.håndter(varselbrevBehov)
    }

    fun trengerBrukerinfo() {
        bruker!!.trengerBrukerinfo(behovObservatør, eksternFagsak.ytelse)
    }

    fun trengerIverksettelse() {
        behandlingHistorikk.nåværende().entry.trengerIverksettelse(
            behovObservatør,
            ytelsestype = eksternFagsak.ytelse.tilYtelsestype(),
            aktør = requireNotNull(bruker) { "Aktør kreves for Iverksettelse." }.aktør,
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
        )
    }

    fun hentFagsysteminfo(): Ytelse {
        return eksternFagsak.ytelse
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

    fun faktastegFrontendDto(): FaktaFeilutbetalingDto = behandlingHistorikk.nåværende().entry.faktastegFrontendDto(opprettelsesvalg, opprettet)

    fun håndter(
        beslutter: Behandler,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(beslutter, vurderinger, this)
        if (behandling.kanUtbetales()) {
            byttTilstand(IverksettVedtak)
        }
        behandling.utførSideeffekt(tilstand, this)
    }

    fun håndter(
        behandler: Behandler,
        vurdering: Faktasteg.Vurdering,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, vurdering, this)
        behandling.utførSideeffekt(tilstand, this)
    }

    fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, periode, vurdering, this)
        behandling.utførSideeffekt(tilstand, this)
    }

    fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, periode, vurdering, this)
        behandling.utførSideeffekt(tilstand, this)
    }

    fun håndterForeslåVedtak(
        behandler: Behandler,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndterForeslåVedtak(
            behandler,
            this,
        )
        behandling.utførSideeffekt(tilstand, this)
    }

    fun håndter(
        behandler: Behandler,
        brevmottaker: RegistrertBrevmottaker,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, brevmottaker, this)
        behandling.utførSideeffekt(tilstand, this)
    }

    fun aktiverBrevmottakerSteg() {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.aktiverBrevmottakerSteg()
        behandling.utførSideeffekt(tilstand, this)
    }

    fun deaktiverBrevmottakerSteg() = {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.deaktiverBrevmottakerSteg()
        behandling.utførSideeffekt(tilstand, this)
    }

    fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.fjernManuelBrevmottaker(behandler, manuellBrevmottakerId, this)
        behandling.utførSideeffekt(tilstand, this)
    }

    fun frontendDtoForBehandling(
        behandler: Behandler,
        kanBeslutte: Boolean,
    ) = behandlingHistorikk.nåværende().entry.tilFrontendDto(tilstand, behandler, kanBeslutte)

    fun frontendDtoForBehandlingsoppsummering() = behandlingHistorikk.tilOppsummeringDto(tilstand)

    fun tilEntity(): TilbakekrevingEntity {
        return TilbakekrevingEntity(
            nåværendeTilstand = tilstand.tilbakekrevingTilstand,
            id = this.id,
            eksternFagsak = this.eksternFagsak.tilEntity(id),
            behandlingHistorikkEntities = this.behandlingHistorikk.tilEntity(id),
            kravgrunnlagHistorikkEntities = this.kravgrunnlagHistorikk.tilEntity(id),
            brevHistorikkEntities = this.brevHistorikk.tilEntity(),
            opprettet = this.opprettet,
            opprettelsesvalg = this.opprettelsesvalg,
            bruker = this.bruker?.tilEntity(),
        )
    }

    fun hentTilbakekrevingUrl(baseUrl: String): String {
        val behandling = requireNotNull(behandlingHistorikk.nåværende())
        val fagsystem = eksternFagsak.ytelse.tilFagsystemDTO()

        return URLBuilder(baseUrl).apply {
            path(
                "fagsystem",
                fagsystem.toString(),
                "fagsak",
                eksternFagsak.eksternId,
                "behandling",
                behandling.entry.id.toString(),
            )
        }.buildString()
    }

    override fun behandlingOppdatert(
        behandlingId: UUID,
        eksternBehandlingId: String,
        vedtaksresultat: Vedtaksresultat?,
        behandlingstatus: Behandlingsstatus,
        venterPåBruker: Boolean,
        ansvarligSaksbehandler: String,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal?,
        totalFeilutbetaltPeriode: Datoperiode?,
    ) {
        endringObservatør.behandlingsstatusOppdatert(
            behandlingId = behandlingId,
            forrigeBehandlingId = behandlingHistorikk.forrige()?.entry?.id,
            eksternFagsystemId = eksternFagsak.eksternId,
            eksternBehandlingId = eksternBehandlingId,
            ytelse = eksternFagsak.ytelse,
            tilstand = tilstand.tilbakekrevingTilstand,
            behandlingstatus = behandlingstatus,
            vedtaksresultat = vedtaksresultat,
            venterPåBruker = venterPåBruker,
            ansvarligEnhet = null,
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            ansvarligBeslutter = ansvarligBeslutter,
            totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
            totalFeilutbetaltPeriode = totalFeilutbetaltPeriode,
        )
    }

    companion object {
        fun opprett(
            id: String,
            behovObservatør: BehovObservatør,
            opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse,
            bigQueryService: BigQueryService,
            endringObservatør: EndringObservatør,
        ): Tilbakekreving {
            val tilbakekreving = Tilbakekreving(
                id = id,
                opprettet = LocalDateTime.now(),
                // TODO: Lesbar ID
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
            )
            tilbakekreving.håndter(opprettTilbakekrevingEvent)
            return tilbakekreving
        }
    }
}
