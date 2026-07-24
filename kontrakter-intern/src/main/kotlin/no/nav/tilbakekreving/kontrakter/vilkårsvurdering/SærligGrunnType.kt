package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

enum class SærligGrunnType(
    val navn: String,
) {
    GRAD_AV_UAKTSOMHET("Graden av uaktsomhet hos den som kravet retter seg mot"),
    STØRRELSE_BELØP("Størrelsen på det feilutbetalte beløpet"),
    TID_FRA_UTBETALING("Hvor lang tid det har gått siden utbetalingen fant sted"),
    HELT_ELLER_DELVIS_NAVS_FEIL("Om feilen helt eller delvis kan tilskrives Nav"),
    ANNET("Annet"),
}
