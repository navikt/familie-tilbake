package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak

import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata

class Vedtaksbrevsdata(
    val vedtaksbrevsdata: HbVedtaksbrevsdata,
    val metadata: Brevmetadata,
) {
    val hovedresultat: Vedtaksresultat get() = vedtaksbrevsdata.felles.hovedresultat
}
