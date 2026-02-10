package no.nav.tilbakekreving.breeeev

enum class PåkrevdBegrunnelse(val tittel: String) {
    UNNLATES_4_RETTSGEBYR("Hvordan har vi kommet frem til at du ikke må betale tilbake?"),
    SKAL_IKKE_UNNLATES_4_RETTSGEBYR("Hvordan har vi kommet frem til at du må betale tilbake?"),
    SÆRLIGE_GRUNNER("Er det særlige grunner til å redusere beløpet?"),
}
