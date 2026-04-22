package no.nav.tilbakekreving.breeeev.standardtekster

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

enum class Lovverk(val bokmål: String, val nynorsk: String, val rekkefølge: Int) {
    FOLKETRYGDLOVEN(
        bokmål = "folketrygdloven",
        nynorsk = "folketrygdlova",
        rekkefølge = 2,
    ),
    FORELDELSESLOVEN(
        bokmål = "foreldelsesloven",
        nynorsk = "foreldelseslova",
        rekkefølge = 3,
    ),
    ARBEIDSMARKEDSLOVEN(
        bokmål = "arbeidsmarkedsloven",
        nynorsk = "arbeidsmarkedslova",
        rekkefølge = 1,
    ),
    BARNETRYGDLOVEN(
        bokmål = "barnetrygdloven",
        nynorsk = "barnetrygdlova",
        rekkefølge = 1,
    ),
    KONTANTSTØTTELOVEN(
        bokmål = "kontantstøtteloven",
        nynorsk = "kontantstøttelova",
        rekkefølge = 1,
    ),
    ;

    fun tekst(språkkode: Språkkode): String = when (språkkode) {
        Språkkode.NB -> bokmål
        Språkkode.NN -> nynorsk
    }
}
