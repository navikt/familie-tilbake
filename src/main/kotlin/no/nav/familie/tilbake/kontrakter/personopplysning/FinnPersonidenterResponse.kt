package no.nav.familie.tilbake.kontrakter.personopplysning

data class FinnPersonidenterResponse(
    val identer: List<PersonIdentMedHistorikk>,
)

data class PersonIdentMedHistorikk(
    val personIdent: String,
    val historisk: Boolean,
)
