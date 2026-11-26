package no.nav.tilbakekreving.breeeev.mottaker

import no.nav.tilbakekreving.entities.AddresseEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.FregAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedmottakerAddresseDto

sealed interface Hovedmottaker : Addresse {
    fun tilFrontendDto(): HovedmottakerAddresseDto

    data object FolkeregistrertAddresse : Hovedmottaker {
        override fun tilFrontendDto(): HovedmottakerAddresseDto {
            return FregAdresseDto
        }

        override fun tilEntity(tilbakekrevingRef: String, navn: String?, tilleggsmottakerType: Tilleggsmottaker.TilleggsmottakerType?): AddresseEntity? {
            return null
        }
    }
}
