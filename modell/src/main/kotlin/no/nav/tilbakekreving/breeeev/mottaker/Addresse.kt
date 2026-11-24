package no.nav.tilbakekreving.breeeev.mottaker

import no.nav.tilbakekreving.entities.AddresseEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.ManuellMottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NorskAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UtenlandskAdresseDto
import java.util.UUID

sealed interface Addresse {
    fun tilEntity(tilbakekrevingRef: String, navn: String?, tilleggsmottakerType: AddresseEntity.TilleggsmottakerType?): AddresseEntity?

    class Norsk(
        override val id: UUID,
        private val addresselinje: String,
        private val postnummer: String,
    ) : ManuellAddresse, Hovedmottaker {
        override fun tilEntity(tilbakekrevingRef: String, navn: String?, tilleggsmottakerType: AddresseEntity.TilleggsmottakerType?): AddresseEntity {
            return AddresseEntity(
                id = id,
                tilbakekrevingRef = tilbakekrevingRef,
                land = null,
                navn = navn,
                type = AddresseEntity.Type.NorskAddresse,
                addresselinje = addresselinje,
                postnummer = postnummer,
                tilleggsmottakerType = tilleggsmottakerType,
                mottakerIdent = null,
            )
        }

        override fun tilFrontendDto(): NorskAdresseDto {
            return NorskAdresseDto(
                gate = addresselinje,
                postnummer = postnummer,
            )
        }
    }

    class Utenlandsk(
        override val id: UUID,
        private val land: String,
        private val addresselinje: String,
    ) : ManuellAddresse, Hovedmottaker {
        override fun tilFrontendDto(): UtenlandskAdresseDto {
            return UtenlandskAdresseDto(
                land = land,
                adresse = addresselinje,
            )
        }

        override fun tilEntity(tilbakekrevingRef: String, navn: String?, tilleggsmottakerType: AddresseEntity.TilleggsmottakerType?): AddresseEntity {
            return AddresseEntity(
                id = id,
                tilbakekrevingRef = tilbakekrevingRef,
                type = AddresseEntity.Type.UtenlandskAddresse,
                navn = navn,
                land = land,
                addresselinje = addresselinje,
                postnummer = null,
                tilleggsmottakerType = tilleggsmottakerType,
                mottakerIdent = null,
            )
        }
    }
}

sealed interface ManuellAddresse : Addresse {
    val id: UUID

    fun tilFrontendDto(): ManuellMottakerDto

    override fun tilEntity(
        tilbakekrevingRef: String,
        navn: String?,
        tilleggsmottakerType: AddresseEntity.TilleggsmottakerType?,
    ): AddresseEntity
}
