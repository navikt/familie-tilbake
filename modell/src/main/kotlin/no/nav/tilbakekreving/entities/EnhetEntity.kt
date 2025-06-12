package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.behandling.Enhet

@Serializable
data class EnhetEntity(
    val kode: String,
    val navn: String,
) {
    fun fraEntity(): Enhet {
        return Enhet(kode, navn)
    }
}
