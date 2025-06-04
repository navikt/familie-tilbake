package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import java.math.BigDecimal

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

object IkkeVurdertEntity : VurderingEntity() {
    override val begrunnelse: String? = null

    override fun fraEntity(): Vilkårsvurderingsteg.Vurdering =
        Vilkårsvurderingsteg.Vurdering.IkkeVurdert
}

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
