package no.nav.tilbakekreving.pdf.dokumentbestilling.felles

class Adresseinfo(
    val ident: String,
    val mottagernavn: String,
    val annenMottagersNavn: String? = null,
    val manuellAdresse: HbManuellAdresse? = null,
)

data class HbManuellAdresse(
    val adresselinje1: String?,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String?,
    val poststed: String?,
    val land: String,
)
