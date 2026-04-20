package no.nav.tilbakekreving.breeeev.standardtekster

import no.nav.tilbakekreving.tekst.slåSammen

enum class HjemmelForTilbakekreving(
    val paragraf: String,
    val lovverk: Lovverk,
) {
    FOLKETRYGDLOVEN_22_15("22-15", Lovverk.FOLKETRYGDLOVEN),
    FOLKETRYGDLOVEN_22_17A("22-17a", Lovverk.FOLKETRYGDLOVEN),
    FORELDELSESLOVEN_2("2", Lovverk.FORELDELSESLOVEN),
    FORELDELSESLOVEN_3("3", Lovverk.FORELDELSESLOVEN),
    FORELDELSESLOVEN_10("10", Lovverk.FORELDELSESLOVEN),
    ARBEIDSMARKEDSLOVEN_22("22", Lovverk.ARBEIDSMARKEDSLOVEN),
    BARNETRYGDLOVEN_13("13", Lovverk.BARNETRYGDLOVEN),
    ;

    companion object {
        fun Iterable<HjemmelForTilbakekreving>.formatter(): String {
            return groupBy { it.lovverk }
                .toList()
                .sortedBy { (lovverk, _) -> lovverk.rekkefølge }
                .map { (lovverk, paragrafer) ->
                    val flereParagrafer = if (paragrafer.size > 1) "§§" else "§"
                    "${lovverk.navn} $flereParagrafer ${paragrafer.map { it.paragraf }.slåSammen()}"
                }
                .slåSammen(" samt ")
        }
    }
}
