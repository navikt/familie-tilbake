package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk

data class KravgrunnlagHistorikkEntity(
    val historikk: List<KravgrunnlagHendelseEntity>,
) {
    fun fraEntity(): KravgrunnlagHistorikk {
        return KravgrunnlagHistorikk(
            historikk = historikk.map { it.fraEntity() }.toMutableList(),
        )
    }
}
