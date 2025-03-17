package no.nav.tilbakekreving.kontrakter.ytelse

enum class Fagsystem(
    val navn: String,
    val tema: String,
) {
    BA("Barnetrygd", "BAR"),
    EF("Enslig forelder", "ENF"),

    @Deprecated(message = "Gyldig verdi for kontantstøtte er KONT")
    KS("Kontantstøtte - gammel", "KON"),

    KONT("Kontantstøtte", "KON"),
    IT01("Infotrygd", ""),
}
