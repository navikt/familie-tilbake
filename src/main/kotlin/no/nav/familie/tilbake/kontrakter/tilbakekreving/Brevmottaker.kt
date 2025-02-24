package no.nav.familie.tilbake.kontrakter.tilbakekreving

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

data class Brevmottaker(
    val type: MottakerType,
    val vergetype: Vergetype? = null,
    @field:Pattern(regexp = "^(.{1,80})\$", message = "Feltet kan ikke være tomt eller innholde mer enn 80 tegn")
    val navn: String,
    @field:Pattern(regexp = "(^$|.{9})", message = "Organisasjonsnummer er ikke riktig")
    val organisasjonsnummer: String? = null,
    @field:Pattern(regexp = "(^$|.{11})", message = "PersonIdent er ikke riktig")
    val personIdent: String? = null,
    @field:Valid
    val manuellAdresseInfo: ManuellAdresseInfo? = null,
) {
    init {
        check(!(manuellAdresseInfo == null && personIdent.isNullOrBlank() && organisasjonsnummer.isNullOrBlank())) {
            "Må ha enten manuellAdresseInfo, personIdent eller et organisasjonsnummer."
        }
        if (type == MottakerType.VERGE || type == MottakerType.FULLMEKTIG) {
            checkNotNull(vergetype) {
                "Vergetype kan ikke være null for brevmottakere av type verge og fullmektig."
            }
        }
    }
}

enum class MottakerType(
    val visningsnavn: String,
) {
    BRUKER_MED_UTENLANDSK_ADRESSE("Bruker med utenlandsk adresse"),
    FULLMEKTIG("Fullmektig"),
    VERGE("Verge"),
    DØDSBO("Dødsbo"),
}

data class ManuellAdresseInfo(
    @field:Pattern(regexp = "^(.{1,80})\$", message = "Feltet kan ikke være tomt eller innholde mer enn 80 tegn")
    val adresselinje1: String,
    @field:Pattern(regexp = "^(.{0,80})\$", message = "Feltet kan ikke inneholde mer enn 80 tegn")
    val adresselinje2: String? = null,
    @field:Pattern(regexp = "^(.{0,10})\$", message = "Feltet kan ikke innholde mer enn 10 tegn")
    val postnummer: String,
    @field:Pattern(regexp = "^(.{0,50})\$", message = "Feltet kan ikke innholde mer enn 50 tegn")
    val poststed: String,
    @field:Pattern(regexp = "^[a-zA-Z]{2}$", message = "Landkode må følge ISO standard, alpha-2")
    val landkode: String,
)
