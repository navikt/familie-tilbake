package no.nav.tilbakekreving.entities

sealed class VurdertAktsomhetEntity {
    abstract val begrunnelse: String
    abstract val skalIleggesRenter: Boolean
    abstract val skalReduseres: SkalReduseresEntity
    abstract val vurderingstype: String
    abstract val type: String
}

data class SimpelUaktsomhetEntity(
    override val begrunnelse: String,
    override val skalReduseres: SkalReduseresEntity,
    val særligGrunner: SærligeGrunnerEntity?,
) : VurdertAktsomhetEntity() {
    override val type = "SimpelUaktsomhet"
    override val vurderingstype: String = "SimpelUaktsomhet"
    override val skalIleggesRenter: Boolean = false
}

data class GrovUaktsomhetEntity(
    override val begrunnelse: String,
    override val skalReduseres: SkalReduseresEntity,
    override val skalIleggesRenter: Boolean,
    val særligGrunner: SærligeGrunnerEntity?,
) : VurdertAktsomhetEntity() {
    override val vurderingstype: String = "GrovUaktsomhet"
    override val type: String = "GrovUaktsomhet"
}

data class ForsettEntity(
    override val begrunnelse: String,
    override val skalIleggesRenter: Boolean,
) : VurdertAktsomhetEntity() {
    override val skalReduseres: SkalReduseresEntity = NeiEntitySkalReduseres
    override val vurderingstype: String = "Forsett"
    override val type: String = "Forsett"
}

data class SærligeGrunnerEntity(
    val begrunnelse: String,
    val grunner: List<String>,
)
