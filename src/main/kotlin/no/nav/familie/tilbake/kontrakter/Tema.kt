package no.nav.familie.tilbake.kontrakter

enum class Tema(
    val fagsaksystem: String,
    val behandlingsnummer: String,
) {
    BAR("BA", "B284"),
    ENF("EF", "B288"),
    KON("KONT", "B278"),
    OPP("OPP", "B288"),
}
