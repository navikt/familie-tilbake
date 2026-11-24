package no.nav.tilbakekreving.breeeev.mottaker

import no.nav.tilbakekreving.entities.AddresseEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.FullmektigDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ManuellAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OrganisasjonDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PersonDto
import no.nav.tilbakekreving.kontrakter.frontend.models.TilleggsmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VergeDto
import java.util.UUID

sealed interface Tilleggsmottaker {
    fun tilEntity(tilbakekrevingRef: String): AddresseEntity

    fun tilFrontendDto(): TilleggsmottakerDto

    data class VergeMedManuellAddresse(
        private val navn: String,
        private val addresse: ManuellAddresse,
    ) : Tilleggsmottaker {
        override fun tilEntity(tilbakekrevingRef: String): AddresseEntity {
            return addresse.tilEntity(
                tilbakekrevingRef = tilbakekrevingRef,
                navn = navn,
                tilleggsmottakerType = AddresseEntity.TilleggsmottakerType.Verge,
            )
        }

        override fun tilFrontendDto(): TilleggsmottakerDto {
            return VergeDto(
                id = addresse.id,
                ManuellAdresseDto(
                    navn = navn,
                    mottaker = addresse.tilFrontendDto(),
                ),
            )
        }
    }

    data class VergeMedFolkeregistrertAddresse(
        private val id: UUID,
        private val fødselsnummer: String,
    ) : Tilleggsmottaker {
        override fun tilEntity(tilbakekrevingRef: String): AddresseEntity {
            return AddresseEntity(
                id = id,
                tilbakekrevingRef = tilbakekrevingRef,
                type = null,
                tilleggsmottakerType = AddresseEntity.TilleggsmottakerType.Verge,
                land = null,
                addresselinje = null,
                postnummer = null,
                navn = null,
                mottakerIdent = fødselsnummer,
            )
        }

        override fun tilFrontendDto(): TilleggsmottakerDto {
            return VergeDto(
                id = id,
                mottaker = PersonDto(
                    navn = "",
                    ident = fødselsnummer,
                ),
            )
        }
    }

    data class FullmektigMedManuellAddresse(
        private val navn: String,
        private val addresse: ManuellAddresse,
    ) : Tilleggsmottaker {
        override fun tilEntity(tilbakekrevingRef: String): AddresseEntity {
            return addresse.tilEntity(
                tilbakekrevingRef = tilbakekrevingRef,
                navn = navn,
                tilleggsmottakerType = AddresseEntity.TilleggsmottakerType.Fullmektig,
            )
        }

        override fun tilFrontendDto(): TilleggsmottakerDto = FullmektigDto(
            id = addresse.id,
            mottaker = ManuellAdresseDto(
                navn = navn,
                mottaker = addresse.tilFrontendDto(),
            ),
        )
    }

    data class FullmektigMedOrganisasjonsnummer(
        private val id: UUID,
        private val navn: String,
        private val organisasjonsnummer: String,
    ) : Tilleggsmottaker {
        override fun tilEntity(tilbakekrevingRef: String): AddresseEntity {
            return AddresseEntity(
                id = id,
                tilbakekrevingRef = tilbakekrevingRef,
                type = null,
                tilleggsmottakerType = AddresseEntity.TilleggsmottakerType.Fullmektig,
                land = null,
                addresselinje = null,
                postnummer = null,
                navn = navn,
                mottakerIdent = organisasjonsnummer,
            )
        }

        override fun tilFrontendDto(): TilleggsmottakerDto {
            return FullmektigDto(
                id = id,
                mottaker = OrganisasjonDto(
                    navn,
                    "",
                    organisasjonsnummer = organisasjonsnummer,
                ),
            )
        }
    }

    data class FullmektigPerson(
        private val id: UUID,
        private val fødselsnummer: String,
    ) : Tilleggsmottaker {
        override fun tilEntity(tilbakekrevingRef: String): AddresseEntity {
            return AddresseEntity(
                id = id,
                tilbakekrevingRef = tilbakekrevingRef,
                type = null,
                tilleggsmottakerType = AddresseEntity.TilleggsmottakerType.Fullmektig,
                land = null,
                addresselinje = null,
                postnummer = null,
                navn = null,
                mottakerIdent = fødselsnummer,
            )
        }

        override fun tilFrontendDto(): TilleggsmottakerDto {
            return FullmektigDto(
                id = id,
                mottaker = PersonDto(
                    navn = "",
                    ident = fødselsnummer,
                ),
            )
        }
    }
}
