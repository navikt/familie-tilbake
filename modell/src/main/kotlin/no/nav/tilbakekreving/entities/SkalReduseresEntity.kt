package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonÅrsaker
import java.math.BigDecimal

data class SkalReduseresEntity(
    val type: SkalReduseresType,
    val prosentdel: Int?,
    val beløpIBehold: BigDecimal? = null,
) {
    fun fraEntity(): ReduksjonÅrsaker.SkalReduseres = when (type) {
        SkalReduseresType.Ja -> ReduksjonÅrsaker.SkalReduseres.Ja(requireNotNull(prosentdel) { "prosentdel kreves for SkalReduseres" })
        SkalReduseresType.JaAvBeløpIBehold -> ReduksjonÅrsaker.SkalReduseres.JaAvBeløpIBehold(
            requireNotNull(prosentdel) { "prosentdel kreves for SkalReduseres av det som er i behold." },
            requireNotNull(beløpIBehold) { "beløpIBehold kreves for SkalReduseres av det som er i behold." },
        )
        SkalReduseresType.Nei -> ReduksjonÅrsaker.SkalReduseres.Nei
    }
}

enum class SkalReduseresType {
    Ja,
    JaAvBeløpIBehold,
    Nei,
}
