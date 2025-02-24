package no.nav.familie.tilbake.kontrakter.simulering

enum class BetalingType(
    val kode: String,
) {
    DEBIT("D"),
    KREDIT("K"),
}
