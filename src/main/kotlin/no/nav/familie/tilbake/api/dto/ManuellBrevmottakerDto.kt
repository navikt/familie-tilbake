package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.MottakerType
import java.util.UUID

data class ManuellBrevmottakerDto(
    val id: UUID? = null,
    val type: MottakerType,
    val navn: String,
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String,
    val poststed: String,
    val landkode: String
)
