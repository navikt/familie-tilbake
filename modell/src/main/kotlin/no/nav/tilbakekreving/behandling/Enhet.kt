package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.entities.EnhetEntity

class Enhet(
    val kode: String,
    val navn: String,
) {
    fun tilEntity(): EnhetEntity {
        return EnhetEntity(kode, navn)
    }
}
