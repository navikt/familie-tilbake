package no.nav.tilbakekreving.kontrakter.tilbakekreving

import no.nav.tilbakekreving.kontrakter.Språkkode
import no.nav.tilbakekreving.kontrakter.Tema

enum class Ytelsestype(
    val kode: String,
    val navn: Map<Språkkode, String>,
) {
    BARNETRYGD(
        "BA",
        mapOf(
            Språkkode.NB to "Barnetrygd",
            Språkkode.NN to "Barnetrygd",
        ),
    ),
    OVERGANGSSTØNAD(
        "EFOG",
        mapOf(
            Språkkode.NB to "Overgangsstønad",
            Språkkode.NN to "Overgangsstønad",
        ),
    ),
    BARNETILSYN(
        "EFBT",
        mapOf(
            Språkkode.NB to "Stønad til barnetilsyn",
            Språkkode.NN to "Stønad til barnetilsyn",
        ),
    ),
    SKOLEPENGER(
        "EFSP",
        mapOf(
            Språkkode.NB to "Stønad til skolepenger",
            Språkkode.NN to "Stønad til skulepengar",
        ),
    ),
    KONTANTSTØTTE(
        "KS",
        mapOf(
            Språkkode.NB to "Kontantstøtte",
            Språkkode.NN to "Kontantstøtte",
        ),
    ),
    ;

    fun tilTema(): Tema =
        when (this) {
            BARNETRYGD -> Tema.BAR
            BARNETILSYN, OVERGANGSSTØNAD, SKOLEPENGER -> Tema.ENF
            KONTANTSTØTTE -> Tema.KON
        }
}
