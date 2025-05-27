package no.nav.tilbakekreving.entities

sealed class SkalReduseresEntity {
    abstract val type: String
}

data class JaEntitySkalReduseres(val prosentdel: Int) : SkalReduseresEntity() {
    override val type: String = "Ja"
}

object NeiEntitySkalReduseres : SkalReduseresEntity() {
    override val type: String = "Nei"
}
