package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

enum class Vilkårsvurderingsresultat(
    val navn: String,
) {
    FORSTO_BURDE_FORSTÅTT("Ja, mottaker forsto eller burde forstått at utbetalingen skyldtes en feil (1. ledd, 1. punkt)"),
    MANGELFULLE_OPPLYSNINGER_FRA_BRUKER(
        "Ja, mottaker har forårsaket feilutbetalingen ved forsett " +
            "eller uaktsomt gitt mangelfulle opplysninger (1. ledd, 2 punkt)",
    ),
    FEIL_OPPLYSNINGER_FRA_BRUKER(
        "Ja, mottaker har forårsaket feilutbetalingen ved forsett eller " +
            "uaktsomt gitt feilaktige opplysninger (1. ledd, 2 punkt)",
    ),
    GOD_TRO("Nei, mottaker har mottatt beløpet i god tro (1. ledd)"),
    UDEFINERT("Ikke Definert"),
}
