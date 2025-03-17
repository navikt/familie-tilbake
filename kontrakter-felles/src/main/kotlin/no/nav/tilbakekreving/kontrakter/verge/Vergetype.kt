package no.nav.tilbakekreving.kontrakter.verge

enum class Vergetype(
    val navn: String,
) {
    VERGE_FOR_BARN("Verge for barn under 18 år"),
    VERGE_FOR_FORELDRELØST_BARN("Verge for foreldreløst barn under 18 år"),
    VERGE_FOR_VOKSEN("Verge for voksen"),
    ADVOKAT("Advokat/advokatfullmektig"),
    ANNEN_FULLMEKTIG("Annen fullmektig"),
    UDEFINERT("Udefinert"),
}
