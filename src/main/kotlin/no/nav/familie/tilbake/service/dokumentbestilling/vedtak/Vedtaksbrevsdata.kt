package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata

class Vedtaksbrevsdata(val vedtaksbrevsdata: HbVedtaksbrevsdata,
                       val metadata: Brevmetadata) {


    val hovedresultat: Vedtaksresultat get() = vedtaksbrevsdata.felles.hovedresultat

}