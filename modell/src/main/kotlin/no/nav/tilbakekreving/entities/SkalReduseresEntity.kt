package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg

@Serializable
sealed class SkalReduseresEntity {
    abstract fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres
}

@Serializable
data class JaEntitySkalReduseres(val prosentdel: Int) : SkalReduseresEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres =
        Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(prosentdel)
}

@Serializable
object NeiEntitySkalReduseres : SkalReduseresEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres =
        Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei
}
