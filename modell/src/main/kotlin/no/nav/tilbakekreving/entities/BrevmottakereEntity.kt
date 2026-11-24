package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.breeeev.mottaker.Brevmottakere
import no.nav.tilbakekreving.breeeev.mottaker.Hovedmottaker

data class BrevmottakereEntity(
    val tilbakekrevingRef: String,
    val hovedmottaker: AddresseEntity?,
    val tilleggsmottaker: AddresseEntity?,
) {
    internal fun fraEntity(): Brevmottakere = Brevmottakere(
        hovedmottaker = hovedmottaker?.let(AddresseEntity::fraEntity) as Hovedmottaker? ?: Hovedmottaker.FolkeregistrertAddresse,
        tilleggsmottaker = tilleggsmottaker?.fraTilleggsmottakerEntity(),
    )
}
