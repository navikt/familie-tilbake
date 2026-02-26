package no.nav.tilbakekreving.breeeev.begrunnelse

enum class VilkårsvurderingBegrunnelse(val tittel: String, val forklaring: String) {
    TILBAKEKREVES(
        tittel = "Hvorfor må du betale tilbake?",
        forklaring = Forklaringstekster.VURDERING_FØRSTE_LEDD,
    ),
    INGEN_TILBAKEKREVING(
        tittel = "Hvorfor må du ikke betale tilbake?",
        forklaring = Forklaringstekster.VURDERING_FØRSTE_LEDD,
    ),
    UNNLATES_4_RETTSGEBYR(
        tittel = "Hvordan har vi kommet frem til at du ikke må betale tilbake?",
        forklaring = "",
    ),
    SKAL_IKKE_UNNLATES_4_RETTSGEBYR(
        tittel = "Hvordan har vi kommet frem til at du må betale tilbake?",
        forklaring = "",
    ),
    IKKE_REDUSERT_SÆRLIGE_GRUNNER(
        tittel = "Hvorfor har vi ikke redusert beløpet?",
        forklaring = Forklaringstekster.VURDERING_FJERDE_LEDD,
    ),
    REDUSERT_SÆRLIGE_GRUNNER(
        tittel = "Hvorfor har vi redusert beløpet?",
        forklaring = Forklaringstekster.VURDERING_FJERDE_LEDD,
    ),
}
