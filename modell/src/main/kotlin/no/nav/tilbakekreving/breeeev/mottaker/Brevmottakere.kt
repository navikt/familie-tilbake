package no.nav.tilbakekreving.breeeev.mottaker

import no.nav.tilbakekreving.entities.BrevmottakereEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakereDto

class Brevmottakere(
    private val hovedmottaker: Hovedmottaker,
    private val tilleggsmottaker: Tilleggsmottaker?,
) {
    companion object {
        internal fun opprett() = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = null,
        )
    }

    internal fun tilEntity(tilbakekrevingRef: String) = BrevmottakereEntity(
        tilbakekrevingRef = tilbakekrevingRef,
        hovedmottaker = hovedmottaker.tilEntity(tilbakekrevingRef, null, null),
        tilleggsmottaker = tilleggsmottaker?.tilEntity(tilbakekrevingRef),
    )

    fun tilFrontendDto() = BrevmottakereDto(
        hovedmottaker = hovedmottaker.tilFrontendDto(),
        tilleggsmottaker = tilleggsmottaker?.tilFrontendDto(),
    )
}
