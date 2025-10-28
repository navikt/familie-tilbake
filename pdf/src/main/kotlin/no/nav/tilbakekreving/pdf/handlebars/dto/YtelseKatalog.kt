package no.nav.tilbakekreving.pdf.handlebars.dto

import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

interface YtelseKatalog {
    fun infoFor(type: YtelsestypeDTO): Ytelsesinfo
}
