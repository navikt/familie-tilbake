package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk

@Serializable
data class BehandlingHistorikkEntity(
    val historikk: List<BehandlingEntity>,
) {
    fun fraEntity(
        eksternFagsakEntity: EksternFagsakEntity,
        kravgrunnlagHistorikkEntity: KravgrunnlagHistorikkEntity,
    ): BehandlingHistorikk {
        return BehandlingHistorikk(
            historikk = historikk.map { Behandling.fraEntity(it, eksternFagsakEntity, kravgrunnlagHistorikkEntity) }.toMutableList(),
        )
    }
}
