package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import java.math.BigDecimal

data class VurderingEntity(
    val vurderingType: VurderingType,
    val begrunnelse: String?,
    val beløpIBehold: BeløpIBeholdEntity?,
    val aktsomhet: VurdertAktsomhetEntity?,
) {
    fun fraEntity(): Vilkårsvurderingsteg.Vurdering {
        return when (vurderingType) {
            VurderingType.GOD_TRO -> {
                Vilkårsvurderingsteg.Vurdering.GodTro(
                    beløpIBehold =
                        requireNotNull(beløpIBehold) { "beløpbIBehold kreves i GOD_TRO " }.fraEntity(),
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnesle kreves i GOD_TRO " },
                )
            }
            VurderingType.FORSTOD_ELLER_BURDE_FORSTÅTT -> {
                Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått(
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnesle kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " },
                    aktsomhet = requireNotNull(aktsomhet) { "aktsomhet kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " }.fraEntity(),
                )
            }
            VurderingType.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER -> {
                Vilkårsvurderingsteg.Vurdering.MangelfulleOpplysningerFraBruker(
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnelse kreves i MANGELFULLE_OPPLYSNINGER_FRA_BRUKER" },
                    aktsomhet = requireNotNull(aktsomhet) { "aktsomhet kreves i MANGELFULLE_OPPLYSNINGER_FRA_BRUKER" }.fraEntity(),
                )
            }
            VurderingType.FEILAKTIGE_OPPLYSNINGER_FRA_BRUKER -> {
                Vilkårsvurderingsteg.Vurdering.FeilaktigeOpplysningerFraBruker(
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnelse kreves i FEILAKTIGE_OPPLYSNINGER_FRA_BRUKER" },
                    aktsomhet = requireNotNull(aktsomhet) { "aktsomhet kreves i FEILAKTIGE_OPPLYSNINGER_FRA_BRUKER" }.fraEntity(),
                )
            }

            VurderingType.IKKE_VURDERT -> Vilkårsvurderingsteg.Vurdering.IkkeVurdert
        }
    }
}

data class BeløpIBeholdEntity(
    val beholdType: BeholdType,
    val beløp: BigDecimal?,
) {
    fun fraEntity(): Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold {
        return when (beholdType) {
            BeholdType.JA -> {
                Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Ja(requireNotNull(beløp) { "Beløp kreves i BeløpIBehold" })
            }
            BeholdType.NEI -> {
                Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei
            }
        }
    }
}

enum class VurderingType {
    GOD_TRO,
    FORSTOD_ELLER_BURDE_FORSTÅTT,
    MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
    FEILAKTIGE_OPPLYSNINGER_FRA_BRUKER,
    IKKE_VURDERT,
}

enum class BeholdType {
    JA,
    NEI,
}
