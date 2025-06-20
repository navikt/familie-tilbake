package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg

data class SkalReduseresEntity(
    val type: SkalReduseres,
    val prosentdel: Int?,
) {
    fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres = when (type) {
        SkalReduseres.Ja ->
            Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(requireNotNull(prosentdel) { "prosentdel kreves for SkalReduseres" })
        SkalReduseres.Nei -> Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei
    }
}

enum class SkalReduseres {
    Ja,
    Nei,
}
