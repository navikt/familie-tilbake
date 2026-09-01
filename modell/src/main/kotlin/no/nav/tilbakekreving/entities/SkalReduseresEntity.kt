package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter

data class SkalReduseresEntity(
    val type: SkalReduseresType,
    val prosentdel: Int?,
) {
    fun fraEntity(): ReduksjonMomenter.SkalReduseres = when (type) {
        SkalReduseresType.Ja -> ReduksjonMomenter.SkalReduseres.Ja(requireNotNull(prosentdel) { "prosentdel kreves for SkalReduseres" })
        SkalReduseresType.Nei -> ReduksjonMomenter.SkalReduseres.Nei
    }
}

enum class SkalReduseresType {
    Ja,
    Nei,
}
