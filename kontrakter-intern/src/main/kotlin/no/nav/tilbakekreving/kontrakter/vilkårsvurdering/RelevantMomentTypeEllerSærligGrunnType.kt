package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

sealed interface MomentEllerSærligGrunnType {
    val name: String
    val navn: String
}

enum class SærligGrunnType(
    override val navn: String,
) : MomentEllerSærligGrunnType {
    GRAD_AV_UAKTSOMHET("Graden av uaktsomhet hos den som kravet retter seg mot"),
    STØRRELSE_BELØP("Størrelsen på det feilutbetalte beløpet"),
    TID_FRA_UTBETALING("Hvor lang tid det har gått siden utbetalingen fant sted"),
    HELT_ELLER_DELVIS_NAVS_FEIL("Om feilen helt eller delvis kan tilskrives Nav"),
    ANNET("Annet"),
}

enum class RelevantMomentTypeGodTro(
    override val navn: String,
) : MomentEllerSærligGrunnType {
    STØRRELSE_BELØP("Størrelsen på det feilutbetalte beløpet"),
    TID_FRA_UTBETALING("Hvor lang tid det har gått siden feilutbetalingen skjedde"),
    UTBETALING_TILLIT("Om mottakeren har innrettet seg i tillit til utbetalingen"),
    ANNET("Annet"),
}
