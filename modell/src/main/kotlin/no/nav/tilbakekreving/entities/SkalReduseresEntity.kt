package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg

sealed class SkalReduseresEntity {
    abstract fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres
}

data class JaEntitySkalReduseres(val prosentdel: Int) : SkalReduseresEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres =
        Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(prosentdel)
}

object NeiEntitySkalReduseres : SkalReduseresEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres =
        Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei
}
