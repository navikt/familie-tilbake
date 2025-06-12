package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk

@Serializable
data class KravgrunnlagHistorikkEntity(
    val historikk: List<KravgrunnlagHendelseEntity>,
) {
    fun fraEntity(): KravgrunnlagHistorikk {
        return KravgrunnlagHistorikk(
            historikk = historikk.map { it.fraEntity() }.toMutableList(),
        )
    }
}
