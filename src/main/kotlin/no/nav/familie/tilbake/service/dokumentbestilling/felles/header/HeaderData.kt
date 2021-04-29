package no.nav.familie.tilbake.service.dokumentbestilling.felles.header

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Språkstøtte

class HeaderData(override val språkkode: Språkkode,
                 val person: Person,
                 val brev: Brev) : Språkstøtte

