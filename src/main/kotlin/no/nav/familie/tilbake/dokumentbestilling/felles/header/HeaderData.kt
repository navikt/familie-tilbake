package no.nav.familie.tilbake.dokumentbestilling.felles.header

import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Språkstøtte
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

class HeaderData(
    override val språkkode: Språkkode,
    val person: Person,
    val brev: Brev,
    val institusjon: Institusjon? = null,
) : Språkstøtte
