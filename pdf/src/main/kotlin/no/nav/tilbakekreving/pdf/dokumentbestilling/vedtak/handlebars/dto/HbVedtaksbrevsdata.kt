package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.tilbakekreving.pdf.handlebars.dto.Språkstøtte

data class HbVedtaksbrevsdata(
    @get:JsonUnwrapped val felles: HbVedtaksbrevFelles,
    val perioder: List<HbVedtaksbrevsperiode>,
) : Språkstøtte {
    @Suppress("unused") // Handlebars
    val antallPerioder = perioder.size

    override val språkkode: Språkkode = felles.språkkode
}
