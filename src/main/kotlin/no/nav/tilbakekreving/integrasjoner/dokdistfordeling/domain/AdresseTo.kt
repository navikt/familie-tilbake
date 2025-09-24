package no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain

import kotlinx.serialization.Serializable

@Serializable
data class AdresseTo(
    val adressetype: String,
    val postnummer: String?,
    val poststed: String?,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val land: String,
)
