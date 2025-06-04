package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Enhet

data class EnhetEntity(
    val kode: String,
    val navn: String,
) {
    fun fraEntity(): Enhet {
        return Enhet(kode, navn)
    }
}
