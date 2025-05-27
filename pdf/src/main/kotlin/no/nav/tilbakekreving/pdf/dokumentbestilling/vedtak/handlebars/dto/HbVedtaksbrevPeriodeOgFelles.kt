package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.tilbakekreving.pdf.handlebars.dto.Språkstøtte

class HbVedtaksbrevPeriodeOgFelles(
    @get:JsonUnwrapped
    val felles: HbVedtaksbrevFelles,
    @get:JsonUnwrapped
    val periode: HbVedtaksbrevsperiode,
) : Språkstøtte {
    override val språkkode: Språkkode = felles.språkkode
}
