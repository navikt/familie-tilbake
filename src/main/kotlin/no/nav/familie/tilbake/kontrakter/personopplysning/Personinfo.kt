package no.nav.familie.tilbake.kontrakter.personopplysning

data class Adressebeskyttelse(
    val gradering: ADRESSEBESKYTTELSEGRADERING,
)

enum class ADRESSEBESKYTTELSEGRADERING {
    STRENGT_FORTROLIG_UTLAND, // Kode 19
    STRENGT_FORTROLIG, // Kode 6
}
