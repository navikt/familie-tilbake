package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.BehandlingsoppsummeringDto
import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.person.Bruker
import no.nav.tilbakekreving.person.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.Tilstand
import java.time.LocalDateTime

class Tilbakekreving(
    val eksternFagsak: EksternFagsak,
    val behandlingHistorikk: BehandlingHistorikk,
    private val opprettet: LocalDateTime,
    private val behovObservatør: BehovObservatør,
    private var bruker: Bruker? = null,
) : FrontendDto<FagsakDto> {
    var tilstand: Tilstand = Start

    fun byttTilstand(nyTilstand: Tilstand) {
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

    fun trengerVarselbrev() {
        behovObservatør.håndter(VarselbrevBehov("wip"))
    }

    override fun tilFrontendDto(): FagsakDto {
        val eksternFagsakDto = eksternFagsak.tilFrontendDto()
        return FagsakDto(
            eksternFagsakId = eksternFagsakDto.eksternId,
            ytelsestype = eksternFagsakDto.ytelsestype,
            fagsystem = eksternFagsakDto.fagsystem,
            språkkode = bruker?.språkkode ?: Språkkode.NB,
            bruker = bruker.tilNullableFrontendDto(),
            behandlinger =
                behandlingHistorikk.tilFrontendDto().map {
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
                eksternFagsak =
                    EksternFagsak(
                        eksternId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                        ytelsestype = opprettTilbakekrevingEvent.eksternFagsak.ytelsestype,
                        fagsystem = opprettTilbakekrevingEvent.eksternFagsak.fagsystem,
                        behovObservatør = behovObservatør,
                        behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
                    ),
                behovObservatør = behovObservatør,
                behandlingHistorikk = BehandlingHistorikk(mutableListOf()),
            )
        }
    }
}
