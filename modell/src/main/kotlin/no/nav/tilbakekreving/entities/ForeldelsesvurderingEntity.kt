package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg.Vurdering
import java.time.LocalDate

data class ForeldelsesvurderingEntity(
    val type: String,
    val begrunnelse: String? = null,
    val frist: LocalDate? = null,
    val oppdaget: LocalDate? = null,
) {
    fun fraEntity(): Vurdering {
        val vurdering = when {
            type.contains("IKKE_FORELDET") -> Vurdering.IkkeForeldet(begrunnelse!!)
            type.contains("IKKE_VURDERT") -> Vurdering.IkkeVurdert
            type.contains("TILLEGGSFRIST") -> Vurdering.Tilleggsfrist(frist!!, oppdaget!!)
            type.contains("FORELDET") -> Vurdering.Foreldet(begrunnelse!!, frist!!)
            else -> throw IllegalArgumentException("Ugyldig type: $type")
        }
        return vurdering
    }
}
