package no.nav.tilbakekreving.pdf.handlebars.dto

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

data class Ytelsesinfo(
    val url: String,
    val navn: Map<Språkkode, Ytelsesnavn>,
)

data class Ytelsesnavn(
    val ubestemt: String,
    val bestemt: String,
    val eiendomsform: String,
)

fun Ytelsesinfo.navnFor(lang: Språkkode): Ytelsesnavn =
    navn[lang] ?: navn[Språkkode.NB]
        ?: error("Mangler språkinnhold for $lang og NB")
