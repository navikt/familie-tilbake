package no.nav.tilbakekreving

import io.ktor.http.URLBuilder
import io.ktor.http.path
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.aktør.Bruker
import no.nav.tilbakekreving.aktør.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behandling.BehandlingObservatør
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
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
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.IverksettVedtak
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.Tilstand
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class Tilbakekreving internal constructor(
    val id: UUID,
    val fagsystemId: String,
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
        behandlingHistorikk.nåværende().entry.utførSideeffekt(this)
    }

    fun sporingsinformasjon(): Sporing {
        return Sporing(eksternFagsak.eksternId, behandlingHistorikk.nåværende().entry.internId.toString())
    }

    fun håndterNullstilling() = tilstand.håndterNullstilling(this)

    fun nullstillBehandling() {
        val nåværendeBehandling = behandlingHistorikk.nåværende().entry
        val nullstiltBehandling = nåværendeBehandling.lagNullstiltBehandling(brevHistorikk, this)
        behandlingHistorikk.lagre(nullstiltBehandling)
    }

    fun opprettBrevmottakerSteg(
        navn: String,
        ident: String,
    ) {
        behandlingHistorikk.nåværende().entry.opprettBrevmottaker(navn, ident)
    }

    fun opprettBehandling(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        behandler: Behandler,
    ) {
        val behandlingId = UUID.randomUUID()
        val behandling = Behandling.nyBehandling(
            internId = behandlingId,
            eksternId = behandlingId,
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            opprettet = LocalDateTime.now(),
            enhet = null,
            årsak = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
            ansvarligSaksbehandler = behandler,
            sistEndret = LocalDateTime.now(),
            eksternFagsakBehandling = eksternFagsakBehandling,
            kravgrunnlag = kravgrunnlagHistorikk.nåværende(),
            brevHistorikk = brevHistorikk,
            behandlingObservatør = this,
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

    fun trengerVarselbrev() {
        behovObservatør.håndter(VarselbrevBehov("wip"))
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
            behandlinger = behandlingHistorikk.tilOppsummeringDto(),
        )
    }

    fun håndter(
        beslutter: Behandler,
        vurderinger: List<Pair<Behandlingssteg, FatteVedtakSteg.Vurdering>>,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(beslutter, vurderinger, this)
        if (behandling.kanUtbetales()) {
            byttTilstand(IverksettVedtak)
        }
        behandling.utførSideeffekt(this)
    }

    fun håndter(
        behandler: Behandler,
        vurdering: Faktasteg.Vurdering,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, vurdering, this)
        behandling.utførSideeffekt(this)
    }

    fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, periode, vurdering, this)
        behandling.utførSideeffekt(this)
    }

    fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, periode, vurdering, this)
        behandling.utførSideeffekt(this)
    }

    fun håndter(
        behandler: Behandler,
        vurdering: ForeslåVedtakSteg.Vurdering,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, vurdering, this)
        behandling.utførSideeffekt(this)
    }

    fun håndter(
        behandler: Behandler,
        brevmottaker: RegistrertBrevmottaker,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.håndter(behandler, brevmottaker, this)
        behandling.utførSideeffekt(this)
    }

    fun aktiverBrevmottakerSteg() {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.aktiverBrevmottakerSteg()
        behandling.utførSideeffekt(this)
    }

    fun deaktiverBrevmottakerSteg() = {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.deaktiverBrevmottakerSteg()
        behandling.utførSideeffekt(this)
    }

    fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
    ) {
        val behandling = behandlingHistorikk.nåværende().entry
        behandling.fjernManuelBrevmottaker(behandler, manuellBrevmottakerId, this)
        behandling.utførSideeffekt(this)
    }

    fun tilEntity(): TilbakekrevingEntity {
        return TilbakekrevingEntity(
            nåværendeTilstand = tilstand.tilbakekrevingTilstand,
            id = this.id,
            fagsystemId = fagsystemId,
            eksternFagsak = this.eksternFagsak.tilEntity(),
            behandlingHistorikkEntities = this.behandlingHistorikk.tilEntity(),
            kravgrunnlagHistorikkEntities = this.kravgrunnlagHistorikk.tilEntity(),
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
                behandling.entry.internId.toString(),
            )
        }.buildString()
    }

    override fun behandlingOppdatert(
        behandlingId: UUID,
        eksternBehandlingId: String,
        vedtaksresultat: Vedtaksresultat?,
        behandlingstatus: Behandlingsstatus,
        venterPåBruker: Boolean,
        ansvarligSaksbehandler: String?,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal?,
        totalFeilutbetaltPeriode: Datoperiode?,
    ) {
        endringObservatør.behandlingsstatusOppdatert(
            behandlingId = behandlingId,
            forrigeBehandlingId = behandlingHistorikk.forrige()?.entry?.internId,
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
            behovObservatør: BehovObservatør,
            opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse,
            bigQueryService: BigQueryService,
            endringObservatør: EndringObservatør,
        ): Tilbakekreving {
            val tilbakekreving = Tilbakekreving(
                id = UUID.randomUUID(),
                opprettet = LocalDateTime.now(),
                // TODO: Lesbar ID
                fagsystemId = UUID.randomUUID().toString(),
                opprettelsesvalg = opprettTilbakekrevingEvent.opprettelsesvalg,
                eksternFagsak = EksternFagsak(
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
