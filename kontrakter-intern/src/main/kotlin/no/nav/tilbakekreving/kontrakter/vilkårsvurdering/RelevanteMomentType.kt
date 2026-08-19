package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

enum class RelevanteMomentType(
    val navn: String,
) {
    STØRRELSE_BELØP("Størrelsen på det feilutbetalte beløpet"),
    TID_FRA_UTBETALING("Hvor lang tid det har gått siden utbetalingen fant sted"),
    UTBETALING_TILLIT("Om mottakeren har innrettet seg i tillit til utbetalingen"),
    ANNET("Annet"),
}
