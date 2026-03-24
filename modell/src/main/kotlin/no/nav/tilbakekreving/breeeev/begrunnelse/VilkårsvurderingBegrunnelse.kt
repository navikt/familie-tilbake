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
    GOD_TRO_BELØP_I_BEHOLD(
        tittel = "Hvorfor må du betale tilbake?",
        forklaring = Forklaringstekster.GOD_TRO_BELØP_I_BEHOLD,
    ),
    GOD_TRO_BELØP_IKKE_I_BEHOLD(
        tittel = "Hvorfor må du ikke betale tilbake?",
        forklaring = Forklaringstekster.GOD_TRO_BELØP_IKKE_I_BEHOLD,
    ),
    UNNLATES_4_RETTSGEBYR(
        tittel = "Placeholder - skal unnlates - 4 rettsgebyr",
        forklaring = "",
    ),
    SKAL_IKKE_UNNLATES_4_RETTSGEBYR(
        tittel = "Placeholder - skal ikke unnlates - 4 rettsgebyr",
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
