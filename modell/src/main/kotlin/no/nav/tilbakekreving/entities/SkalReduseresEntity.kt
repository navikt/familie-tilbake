package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg

data class SkalReduseresEntity(
    val type: SkalReduseresType,
    val prosentdel: Int?,
) {
    fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres = when (type) {
        SkalReduseresType.Ja ->
            Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(requireNotNull(prosentdel) { "prosentdel kreves for SkalReduseres" })
        SkalReduseresType.Nei -> Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei
    }
}

enum class SkalReduseresType {
    Ja,
    Nei,
}
