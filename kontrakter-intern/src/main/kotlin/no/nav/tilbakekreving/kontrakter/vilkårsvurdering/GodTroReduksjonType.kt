package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

enum class GodTroReduksjonType(
    val navn: String,
) {
    STØRRELSE_BELØP("Størrelsen på beløpet"),
    TID_FRA_UTBETALING("Hvor lenge siden feilutbetalingen skjedde"),
    MOTTAKER_TILLIT("Om mottakeren har innrettet seg i tillit til utbetalingen"),
    ANNET("Annet"),
}
