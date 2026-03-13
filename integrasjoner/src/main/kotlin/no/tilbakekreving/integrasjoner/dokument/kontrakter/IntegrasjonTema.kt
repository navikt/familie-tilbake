package no.tilbakekreving.integrasjoner.dokument.kontrakter

enum class IntegrasjonTema(
    val fagsaksystem: String,
    val behandlingsnummer: String,
) {
    BAR("BA", "B284"),
    ENF("EF", "B288"),
    KON("KONT", "B278"),
    AAP("AAP", "B287"),
    OPP("OPP", "B288"),
    TSO("TSO", "B289"),
    IND("TILTAKSPENGER", "B470"),
    DAG("DAG", "B286"), // TODO: Bekreft behandlingsnummer B286 for dagpenger tilbakekreving
}
