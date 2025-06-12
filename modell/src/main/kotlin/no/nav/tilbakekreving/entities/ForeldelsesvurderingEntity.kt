package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg.Vurdering
import java.time.LocalDate

@Serializable
data class ForeldelsesvurderingEntity(
    val type: String,
    val begrunnelse: String? = null,
    val frist: String? = null,
    val oppdaget: String? = null,
) {
    fun fraEntity(): Vurdering {
        val vurdering = when {
            type.contains("IKKE_FORELDET") -> Vurdering.IkkeForeldet(begrunnelse!!)
            type.contains("IKKE_VURDERT") -> Vurdering.IkkeVurdert
            type.contains("TILLEGGSFRIST") -> Vurdering.Tilleggsfrist(LocalDate.parse(frist!!), LocalDate.parse(oppdaget!!))
            type.contains("FORELDET") -> Vurdering.Foreldet(begrunnelse!!)
            else -> throw IllegalArgumentException("Ugyldig type: $type")
        }
        return vurdering
    }
}
