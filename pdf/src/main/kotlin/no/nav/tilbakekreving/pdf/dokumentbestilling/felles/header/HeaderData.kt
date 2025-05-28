package no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.handlebars.dto.Språkstøtte

class HeaderData(
    override val språkkode: Språkkode,
    val person: Person,
    val brev: Brev,
    val institusjon: Institusjon? = null,
) : Språkstøtte
