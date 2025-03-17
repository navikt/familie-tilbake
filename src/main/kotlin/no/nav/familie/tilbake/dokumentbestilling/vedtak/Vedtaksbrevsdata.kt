package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat

class Vedtaksbrevsdata(
    val vedtaksbrevsdata: HbVedtaksbrevsdata,
    val metadata: Brevmetadata,
) {
    val hovedresultat: Vedtaksresultat get() = vedtaksbrevsdata.felles.hovedresultat
}
