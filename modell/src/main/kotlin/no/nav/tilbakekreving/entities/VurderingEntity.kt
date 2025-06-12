package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GodTroEntity::class, name = "GodTro"),
    JsonSubTypes.Type(value = ForstodEllerBurdeForståttEntity::class, name = "ForstodEllerBurdeForstått"),
    JsonSubTypes.Type(value = MangelfulleOpplysningerFraBrukerEntity::class, name = "MangelfulleOpplysningerFraBruker"),
    JsonSubTypes.Type(value = FeilaktigeOpplysningerFraBrukerEntity::class, name = "FeilaktigeOpplysningerFraBruker"),
    JsonSubTypes.Type(value = IkkeVurdertEntity::class, name = "IkkeVurdert"),
)
@Serializable
sealed class VurderingEntity {
    abstract val begrunnelse: String?

    abstract fun fraEntity(): Vilkårsvurderingsteg.Vurdering
}

@Serializable
data class GodTroEntity(
    val beløpIBehold: BeløpIBeholdEntity,
    override val begrunnelse: String,
) : VurderingEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering {
        return Vilkårsvurderingsteg.Vurdering.GodTro(
            beløpIBehold = beløpIBehold.fraEntity(),
            begrunnelse = begrunnelse,
        )
    }
}

@Serializable
data class ForstodEllerBurdeForståttEntity(
    override val begrunnelse: String,
    val aktsomhet: VurdertAktsomhetEntity,
) : VurderingEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering {
        return Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått(
            begrunnelse = begrunnelse,
            aktsomhet = aktsomhet.fraEntity(),
        )
    }
}

@Serializable
data class MangelfulleOpplysningerFraBrukerEntity(
    override val begrunnelse: String,
    val aktsomhet: VurdertAktsomhetEntity,
) : VurderingEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering {
        return Vilkårsvurderingsteg.Vurdering.MangelfulleOpplysningerFraBruker(
            begrunnelse = begrunnelse,
            aktsomhet = aktsomhet.fraEntity(),
        )
    }
}

@Serializable
data class FeilaktigeOpplysningerFraBrukerEntity(
    override val begrunnelse: String,
    val aktsomhet: VurdertAktsomhetEntity,
) : VurderingEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering {
        return Vilkårsvurderingsteg.Vurdering.FeilaktigeOpplysningerFraBruker(
            begrunnelse = begrunnelse,
            aktsomhet = aktsomhet.fraEntity(),
        )
    }
}

@Serializable
data class IkkeVurdertEntity(
    override val begrunnelse: String? = null,
) : VurderingEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering =
        Vilkårsvurderingsteg.Vurdering.IkkeVurdert
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BeløpIBeholdJaEntity::class, name = "Ja"),
    JsonSubTypes.Type(value = BeløpIBeholdNeiEntity::class, name = "Nei"),
)
@Serializable
sealed class BeløpIBeholdEntity {
    abstract fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold
}

@Serializable
data class BeløpIBeholdJaEntity(
    val beløp: String,
) : BeløpIBeholdEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold =
        Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Ja(BigDecimal(beløp))
}

@Serializable
object BeløpIBeholdNeiEntity : BeløpIBeholdEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold =
        Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei
}
