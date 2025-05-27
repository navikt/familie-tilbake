package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.person.Bruker
import no.nav.tilbakekreving.person.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.Tilstand
import java.time.LocalDateTime
import java.util.UUID

class Tilbakekreving(
    val id: UUID = UUID.randomUUID(),
    val fagsystemId: String,
    val eksternFagsak: EksternFagsak,
    val behandlingHistorikk: BehandlingHistorikk,
    val kravgrunnlagHistorikk: KravgrunnlagHistorikk,
    val brevHistorikk: BrevHistorikk,
    val opprettet: LocalDateTime,
    val opprettelsesvalg: Opprettelsesvalg,
    private val behovObservatør: BehovObservatør,
    var bruker: Bruker? = null,
) : FrontendDto<FagsakDto> {
    internal var tilstand: Tilstand = Start

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

    fun håndterNullstilling() = tilstand.håndterNullstilling(this)

    fun nullstillBehandling() {
        val nåværendeBehandling = behandlingHistorikk.nåværende().entry
        val nullstiltBehandling = nåværendeBehandling.lagNullstiltBehandling(brevHistorikk)
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
        behandlingHistorikk.lagre(
            Behandling.nyBehandling(
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
            ),
        )
    }

    fun opprettBruker(ident: String) {
        this.bruker = Bruker(
            ident = ident,
        )
    }

    fun trengerVarselbrev() {
        behovObservatør.håndter(VarselbrevBehov("wip"))
    }

    fun trengerBrukerinfo() {
        bruker!!.trengerBrukerinfo(behovObservatør, eksternFagsak.ytelse)
    }

    fun trengerIverksettelse() {
        behandlingHistorikk.nåværende().entry.trengerIverksettelse(behovObservatør)
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
            behandlinger = behandlingHistorikk.tilFrontendDto().map {
                BehandlingsoppsummeringDto(it.behandlingId, it.eksternBrukId, it.type, it.status)
            },
        )
    }

    fun håndter(
        beslutter: Behandler,
        behandlingssteg: Behandlingssteg,
        vurdering: FatteVedtakSteg.Vurdering,
    ) {
        behandlingHistorikk.nåværende().entry.håndter(this, beslutter, behandlingssteg, vurdering)
    }

    fun håndter(
        behandler: Behandler,
        vurdering: FaktaFeilutbetalingsperiodeDto,
    ) = behandlingHistorikk.nåværende().entry.håndter(behandler, vurdering)

    fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Foreldelsesteg.Vurdering,
    ) = behandlingHistorikk.nåværende().entry.håndter(behandler, periode, vurdering)

    fun håndter(
        behandler: Behandler,
        periode: Datoperiode,
        vurdering: Vilkårsvurderingsteg.Vurdering,
    ) = behandlingHistorikk.nåværende().entry.håndter(behandler, periode, vurdering)

    fun håndter(
        behandler: Behandler,
        vurdering: ForeslåVedtakSteg.Vurdering,
    ) = behandlingHistorikk.nåværende().entry.håndter(behandler, vurdering)

    fun håndter(
        behandler: Behandler,
        brevmottaker: RegistrertBrevmottaker,
    ) = behandlingHistorikk.nåværende().entry.håndter(behandler, brevmottaker)

    fun aktiverBrevmottakerSteg() = behandlingHistorikk.nåværende().entry.aktiverBrevmottakerSteg()

    fun deaktiverBrevmottakerSteg() = behandlingHistorikk.nåværende().entry.deaktiverBrevmottakerSteg()

    fun fjernManuelBrevmottaker(
        behandler: Behandler,
        manuellBrevmottakerId: UUID,
    ) {
        behandlingHistorikk.nåværende().entry.fjernManuelBrevmottaker(behandler, manuellBrevmottakerId)
    }

    fun tilEntity(): TilbakekrevingEntity {
        return TilbakekrevingEntity(
            nåværendeTilstand = tilstand.navn,
            id = this.id,
            eksternFagsak = this.eksternFagsak.tilEntity(),
            behandlingHistorikk = this.behandlingHistorikk.tilEntity(),
            kravgrunnlagHistorikk = this.kravgrunnlagHistorikk.tilEntity(),
            brevHistorikk = this.brevHistorikk.tilEntity(),
            opprettet = this.opprettet,
            opprettelsesvalg = opprettelsesvalg.name,
            bruker = bruker?.tilEntity(),
        )
    }

    companion object {
        fun opprett(
            behovObservatør: BehovObservatør,
            opprettTilbakekrevingEvent: OpprettTilbakekrevingHendelse,
        ): Tilbakekreving {
            val tilbakekreving = Tilbakekreving(
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
            )
            tilbakekreving.håndter(opprettTilbakekrevingEvent)
            return tilbakekreving
        }
    }
}
