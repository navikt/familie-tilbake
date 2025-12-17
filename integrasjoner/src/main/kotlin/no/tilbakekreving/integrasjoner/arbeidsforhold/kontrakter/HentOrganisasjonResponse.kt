package no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter

data class HentOrganisasjonResponse(
    val navn: Navn,
    val adresse: OrganisasjonAdresse?,
)

data class OrganisasjonAdresse(
    val type: String,
    val adresselinje1: String?,
    val adresselinje2: String?,
    val adresselinje3: String?,
    val postnummer: String?,
    val poststed: String?,
    val kommunenummer: String?,
    val landkode: String?,
)

data class Navn(
    val sammensattnavn: String,
)
