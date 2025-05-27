package no.nav.tilbakekreving.entities

import java.math.BigDecimal

sealed class VurderingEntity {
    abstract val type: String
    abstract val begrunnelse: String?
}

data class GodTroEntity(
    val beløpIBehold: BeløpIBeholdEntity,
    override val begrunnelse: String,
) : VurderingEntity() {
    override val type: String = "GodTro"
}

data class FeilaktigeOpplysningerFraBrukerEntity(
    override val begrunnelse: String,
    val aktsomhet: VurdertAktsomhetEntity,
) : VurderingEntity() {
    override val type: String = "FeilaktigeOpplysningerFraBruker"
}

data class MangelfulleOpplysningerFraBrukerEntity(
    override val begrunnelse: String,
    val aktsomhet: VurdertAktsomhetEntity,
) : VurderingEntity() {
    override val type: String = "MangelfulleOpplysningerFraBruker"
}

data class ForstodEllerBurdeForståttEntity(
    override val begrunnelse: String,
    val aktsomhet: VurdertAktsomhetEntity,
) : VurderingEntity() {
    override val type: String = "ForstodEllerBurdeForstått"
}

object IkkeVurdertEntity : VurderingEntity() {
    override val begrunnelse: String? = null
    override val type: String = "IkkeVurdert"
}

sealed class BeløpIBeholdEntity {
    abstract val type: String
}

data class JaEntity(val beløp: BigDecimal) : BeløpIBeholdEntity() {
    override val type = "Ja"
}

object NeiEntity : BeløpIBeholdEntity() {
    override val type = "Nei"
}
