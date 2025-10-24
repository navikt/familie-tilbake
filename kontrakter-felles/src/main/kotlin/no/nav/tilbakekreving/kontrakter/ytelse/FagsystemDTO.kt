package no.nav.tilbakekreving.kontrakter.ytelse

import com.fasterxml.jackson.annotation.JsonAlias

enum class FagsystemDTO {
    BA,
    EF,
    AAP,

    // TODO: KS alias er deprecated
    @JsonAlias("KS")
    KONT,
    IT01,
    TS,
}
