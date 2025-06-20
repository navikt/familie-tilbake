package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk

data class EksternFagsakEntity(
    val eksternId: String,
    val ytelseEntity: YtelseEntity,
    val behandlinger: List<EksternFagsakBehandlingEntity>,
) {
    fun fraEntity(
        behovObservatør: BehovObservatør,
    ): EksternFagsak {
        val eksternFagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(
            historikk = behandlinger.map { it.fraEntity() }.toMutableList(),
        )
        return EksternFagsak(
            eksternId = eksternId,
            ytelse = ytelseEntity.fraEntity(),
            behandlinger = eksternFagsakBehandlingHistorikk,
            behovObservatør = behovObservatør,
        )
    }
}
