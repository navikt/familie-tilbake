package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
sealed class VurderingEntity {
    abstract val begrunnelse: String?

    abstract fun fraEntity(): Vilkårsvurderingsteg.Vurdering
}

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
sealed class BeløpIBeholdEntity {
    abstract fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold
}

data class BeløpIBeholdJaEntity(val beløp: BigDecimal) : BeløpIBeholdEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold =
        Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Ja(beløp)
}

object BeløpIBeholdNeiEntity : BeløpIBeholdEntity() {
    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold =
        Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei
}
