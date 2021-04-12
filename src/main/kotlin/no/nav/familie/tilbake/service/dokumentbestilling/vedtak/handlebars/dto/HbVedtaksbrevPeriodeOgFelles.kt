package no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Språkstøtte
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode

class HbVedtaksbrevPeriodeOgFelles(@get:JsonUnwrapped
                                   val felles: HbVedtaksbrevFelles,
                                   @get:JsonUnwrapped
                                   val periode: HbVedtaksbrevsperiode) : Språkstøtte {

    override val språkkode: Språkkode = felles.språkkode

}