package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.FagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.person.Bruker
import no.nav.tilbakekreving.person.Bruker.Companion.tilNullableFrontendDto
import no.nav.tilbakekreving.tilstand.Start
import no.nav.tilbakekreving.tilstand.Tilstand
import java.time.LocalDateTime

class Tilbakekreving(
    private val eksternFagsak: EksternFagsak,
    private val opprettet: LocalDateTime,
) : FrontendDto<FagsakDto> {
    var tilstand: Tilstand = Start
    private var bruker: Bruker? = null

    fun byttTilstand(nyTilstand: Tilstand) {
        tilstand = nyTilstand
        tilstand.entering(this)
    }

    fun håndter(opprettTilbakekrevingEvent: OpprettTilbakekrevingEvent) {
        tilstand.håndter(this, opprettTilbakekrevingEvent)
    }

    override fun tilFrontendDto(): FagsakDto {
        val eksternFagsakDto = eksternFagsak.tilFrontendDto()
        return FagsakDto(
            eksternFagsakId = eksternFagsakDto.eksternId,
            ytelsestype = eksternFagsakDto.ytelsestype,
            fagsystem = eksternFagsakDto.fagsystem,
            språkkode = bruker?.språkkode ?: Språkkode.NB,
            bruker = bruker.tilNullableFrontendDto(),
            behandlinger = emptyList(),
        )
    }

    companion object {
        fun opprett(opprettTilbakekrevingEvent: OpprettTilbakekrevingEvent): Tilbakekreving {
            return Tilbakekreving(
                opprettet = LocalDateTime.now(),
                eksternFagsak =
                    EksternFagsak(
                        eksternId = opprettTilbakekrevingEvent.eksternFagsak.eksternId,
                        ytelsestype = opprettTilbakekrevingEvent.eksternFagsak.ytelsestype,
                        fagsystem = opprettTilbakekrevingEvent.eksternFagsak.fagsystem,
                    ),
            )
        }
    }
}
