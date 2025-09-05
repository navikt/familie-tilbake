package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner

data class SkalReduseresEntity(
    val type: SkalReduseresType,
    val prosentdel: Int?,
) {
    fun fraEntity(): ReduksjonSærligeGrunner.SkalReduseres = when (type) {
        SkalReduseresType.Ja -> ReduksjonSærligeGrunner.SkalReduseres.Ja(requireNotNull(prosentdel) { "prosentdel kreves for SkalReduseres" })
        SkalReduseresType.Nei -> ReduksjonSærligeGrunner.SkalReduseres.Nei
    }
}

enum class SkalReduseresType {
    Ja,
    Nei,
}
