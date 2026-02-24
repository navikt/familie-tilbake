package no.nav.tilbakekreving.breeeev

enum class PåkrevdBegrunnelse(val tittel: String, val forklaring: String) {
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
        forklaring = "Her viser du til § 22-15 fjerde ledd, hva som kan være særlige grunner, og hvordan vi  vurderer disse opp mot de faktiske forholdene i saken.",
    ),
    REDUSERT_SÆRLIGE_GRUNNER(
        tittel = "Hvorfor har vi redusert beløpet?",
        forklaring = "Her viser du til § 22-15 fjerde ledd, hva som kan være særlige grunner, og hvordan vi  vurderer disse opp mot de faktiske forholdene i saken.",
    ),
}
