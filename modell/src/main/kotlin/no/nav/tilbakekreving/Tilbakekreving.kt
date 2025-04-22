package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.person.Bruker
import no.nav.tilbakekreving.person.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.Tilstand
import java.time.LocalDateTime
import java.util.UUID

class Tilbakekreving(
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

    fun håndter(opprettTilbakekrevingEvent: OpprettTilbakekrevingEvent) {
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

    fun opprettBehandling(
        eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
        behandler: Behandler,
    ) {
        behandlingHistorikk.lagre(
            Behandling.nyBehandling(
                internId = UUID.fromString("abcdef12-1337-1338-1339-abcdef123456"),
                eksternId = UUID.fromString("abcdef12-1337-1338-1339-abcdef123456"),
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
        bruker!!.trengerBrukerinfo(behovObservatør, eksternFagsak.fagsystem)
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

    companion object {
        fun opprett(
            behovObservatør: BehovObservatør,
            opprettTilbakekrevingEvent: OpprettTilbakekrevingEvent,
        ): Tilbakekreving {
            return Tilbakekreving(
                opprettet = LocalDateTime.now(),
                opprettelsesvalg = opprettTilbakekrevingEvent.opprettelsesvalg,
                eksternFagsak = EksternFagsak(
                    eksternId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                    ytelsestype = opprettTilbakekrevingEvent.eksternFagsak.ytelsestype,
                    fagsystem = opprettTilbakekrevingEvent.eksternFagsak.fagsystem,
                    behovObservatør = behovObservatør,
                    behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
                ),
                behovObservatør = behovObservatør,
                behandlingHistorikk = BehandlingHistorikk(mutableListOf()),
                kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf()),
                brevHistorikk = BrevHistorikk(mutableListOf()),
            )
        }
    }
}
